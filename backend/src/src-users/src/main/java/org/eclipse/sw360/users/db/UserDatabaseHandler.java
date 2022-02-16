/*
 * Copyright Siemens AG, 2013-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users.db;

import com.cloudant.client.api.CloudantClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.UserRepository;
import org.eclipse.sw360.datahandler.db.UserSearchHandler;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.users.dto.APIResponseRedmine;
import org.eclipse.sw360.users.dto.Issue;
import org.eclipse.sw360.users.dto.RedmineConfigDTO;
import org.eclipse.sw360.users.redmine.ReadFileRedmineConfig;
import org.eclipse.sw360.users.util.FileUtil;
import org.ektorp.http.HttpClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.permissions.PermissionUtils.makePermission;

/**
 * Class for accessing the CouchDB database
 *
 * @author cedric.bodet@tngtech.com
 */
public class UserDatabaseHandler {

    /**
     * Connection to the couchDB database
     */
    private DatabaseConnectorCloudant db;
    private DatabaseConnector dbConnector;
    private UserRepository repository;
    private UserSearchHandler userSearchHandler;
    private static final Logger log = LogManager.getLogger(UserDatabaseHandler.class);
    private ReadFileRedmineConfig readFileRedmineConfig;
    private static final String INFO = "INFO";
    private static final String ERROR = "ERROR";
    private static boolean IMPORT_DEPARTMENT_STATUS = false;
    private List<String> departmentDuplicate;
    private List<String> emailDoNotExist;

    public UserDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName) throws IOException {
        // Create the connector
        db = new DatabaseConnectorCloudant(httpClient, dbName);
        dbConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), dbName);
        repository = new UserRepository(db);
        readFileRedmineConfig = new ReadFileRedmineConfig();
        userSearchHandler = new UserSearchHandler(dbConnector, httpClient);
    }

    public UserDatabaseHandler(Supplier<CloudantClient> httpClient, Supplier<HttpClient> client, String dbName) throws IOException {
        // Create the connector
        db = new DatabaseConnectorCloudant(httpClient, dbName);
        dbConnector = new DatabaseConnector(client, dbName);
        repository = new UserRepository(db);
        userSearchHandler = new UserSearchHandler(dbConnector, httpClient);
    }

    public User getByEmail(String email) {
        return repository.getByEmail(email)
                ;
    }

    public User getUser(String id) {
        return db.get(User.class, id);
    }

    private void prepareUser(User user) throws SW360Exception {
        // Prepare component for database
        ThriftValidate.prepareUser(user);
    }

    public RequestStatus addUser(User user) throws SW360Exception {
        prepareUser(user);
        // Add to database
        db.add(user);

        return RequestStatus.SUCCESS;
    }

    public RequestStatus updateUser(User user) throws SW360Exception {
        prepareUser(user);
        db.update(user);

        return RequestStatus.SUCCESS;
    }

    public RequestStatus deleteUser(User user, User adminUser) {
        if (makePermission(user, adminUser).isActionAllowed(RequestedAction.DELETE)) {
            repository.remove(user);
            return RequestStatus.SUCCESS;
        }
        return RequestStatus.FAILURE;
    }

    public List<User> getAll() {
        return repository.getAll();
    }

    public List<User> searchUsers(String searchText) {
        return userSearchHandler.searchByNameAndEmail(searchText);
    }

    public User getByExternalId(String externalId) {
        return repository.getByExternalId(externalId);
    }

    public User getByApiToken(String token) {
        return repository.getByApiToken(token);
    }

    public Set<String> getUserDepartments() {
        return repository.getUserDepartments();
    }

    public Set<String> getUserEmails() {
        return repository.getUserEmails();
    }

    public List<User> search(String text, Map<String, Set<String>> subQueryRestrictions) {
        return userSearchHandler.search(text, subQueryRestrictions);
    }

    public Map<PaginationData, List<User>> getUsersWithPagination(PaginationData pageData) {
        return repository.getUsersWithPagination(pageData);
    }

    public RequestSummary importFileToDB(String pathFolder) {
        String functionName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        departmentDuplicate = new ArrayList<>();
        emailDoNotExist = new ArrayList<>();
        List<Issue> listIssueSuccess = new ArrayList<>();
        List<Issue> listIssueFail = new ArrayList<>();
        RequestSummary requestSummary = new RequestSummary().setTotalAffectedElements(0).setMessage("Import Department Success").setRequestStatus(RequestStatus.SUCCESS);
        RedmineConfigDTO configDTO = readFileRedmineConfig.readFileJson();
        Map<String, List<String>> mapArrayList = new HashMap<>();
        if (IMPORT_DEPARTMENT_STATUS) {
            return requestSummary.setRequestStatus(RequestStatus.PROCESSING);
        }
        IMPORT_DEPARTMENT_STATUS = true;
        try {
            FileUtil.writeLogToFile(INFO, functionName, "START", configDTO.getPathFolderLog());
            Set<String> files = FileUtil.listFilesUsingFileWalk(pathFolder);
            for (String file : files) {
                String pathFile = pathFolder + "/" + file;
                String extension = FilenameUtils.getExtension(pathFile);
                if (extension.equalsIgnoreCase("xlsx") || extension.equalsIgnoreCase("xls")) {
                    mapArrayList = readFileExcel(pathFile);
                } else if (extension.equalsIgnoreCase("csv")) {
                    mapArrayList = readFileCsv(pathFile);
                }
                validateListEmailExistDB(mapArrayList);
                String issueId = pathFile.substring(pathFile.lastIndexOf("_") + 1, pathFile.lastIndexOf("."));
                String fileName = file.replace(pathFile.substring(pathFile.lastIndexOf("_"), pathFile.lastIndexOf(".")), "");
                if (departmentDuplicate.isEmpty() && emailDoNotExist.isEmpty()) {
                    mapArrayList.forEach(this::updateDepartmentToUser);
                    Issue issue = new Issue();
                    String joined = String.join(", ", mapArrayList.keySet());
                    issue.setIssue_id(issueId);
                    issue.setDescription("Department [" + joined + "] added successfully - File: [" + fileName + "]");
                    listIssueSuccess.add(issue);
                } else {
                    if (!departmentDuplicate.isEmpty()) {
                        Issue issueFail = new Issue();
                        issueFail.setIssue_id(issueId);
                        String joined = String.join(", ", departmentDuplicate);
                        issueFail.setDescription("Department [" + joined + "] is duplicate - File: [" + fileName + "]");
                        listIssueFail.add(issueFail);
                    }
                    if (!emailDoNotExist.isEmpty()) {
                        Issue issueFail = new Issue();
                        issueFail.setIssue_id(issueId);
                        String joined = String.join(", ", emailDoNotExist);
                        issueFail.setDescription("User [" + joined + "] does not exist - File: [" + fileName + "]");
                        listIssueFail.add(issueFail);
                    }
                    requestSummary.setRequestStatus(RequestStatus.FAILURE);
                }
            }
            IMPORT_DEPARTMENT_STATUS = false;
            FileUtil.writeLogToFile(INFO, functionName, "END", configDTO.getPathFolderLog());
        } catch (Exception e) {
            IMPORT_DEPARTMENT_STATUS = false;
            FileUtil.writeLogToFile(ERROR, functionName, e.getMessage(), configDTO.getPathFolderLog());
            String msg = "Failed to import department";
            requestSummary.setMessage(msg);
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
        }
        responseData(listIssueSuccess, listIssueFail);
        return requestSummary;
    }

    public Map<String, List<String>> readFileCsv(String filePath) {
        String functionName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        Map<String, List<String>> listMap = new HashMap<>();
        List<String> emailCsv = new ArrayList<>();
        RedmineConfigDTO configDTO = readFileRedmineConfig.readFileJson();
        FileUtil.writeLogToFile(INFO, functionName, "START", configDTO.getPathFolderLog());
        try {
            File file = new File(filePath);
            FileUtil.writeLogToFile(INFO, functionName, file.getName(), configDTO.getPathFolderLog());
            CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(1).build();
            List<String[]> rows = reader.readAll();
            String mapTemp = "";
            for (String[] row : rows) {
                if (row.length > 1) {
                    if (!Objects.equals(row[0], "")) mapTemp = row[0];
                    if (!Objects.equals(row[0], "")) {
                        if (!mapTemp.isEmpty()) {
                            if (listMap.containsKey(mapTemp)) {
                                departmentDuplicate.add(mapTemp);
                            }
                            listMap.put(mapTemp, emailCsv);
                            emailCsv = new ArrayList<>();
                        }
                        mapTemp = row[0];
                    }
                    String email = row[1];
                    emailCsv.add(email);
                }
            }
            if (listMap.containsKey(mapTemp)) {
                departmentDuplicate.add(mapTemp);
            }
            listMap.put(mapTemp, emailCsv);
            FileUtil.writeLogToFile(INFO, functionName, "END", configDTO.getPathFolderLog());
        } catch (IOException | CsvException e) {
            log.error("Can't read file csv: {}", e.getMessage());
            FileUtil.writeLogToFile(ERROR, functionName, e.getMessage(), configDTO.getPathFolderLog());
        }
        return listMap;
    }

    public Map<String, List<String>> readFileExcel(String filePath) {
        String functionName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        RedmineConfigDTO configDTO = readFileRedmineConfig.readFileJson();
        Map<String, List<String>> listMap = new HashMap<>();
        List<String> emailExcel = new ArrayList<>();

        Workbook wb = null;
        FileUtil.writeLogToFile(INFO, functionName, "START", configDTO.getPathFolderLog());
        try (InputStream inp = new FileInputStream(filePath)) {
            FileUtil.writeLogToFile(INFO, functionName, FilenameUtils.getName(filePath), configDTO.getPathFolderLog());
            wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            rows.next();
            String mapTemp = "";
            while (rows.hasNext()) {
                Row row = rows.next();
                if (!isRowEmpty(row)) {
                    // department have value
                    if (row.getCell(0) != null) {
                        if (!mapTemp.isEmpty()) {
                            if (listMap.containsKey(mapTemp)) {
                                departmentDuplicate.add(mapTemp);
                            }
                            listMap.put(mapTemp, emailExcel);
                            emailExcel = new ArrayList<>();
                        }
                        mapTemp = row.getCell(0).getStringCellValue();
                    }
                    String email = row.getCell(1).getStringCellValue();
                    emailExcel.add(email);
                }
            }
            if (listMap.containsKey(mapTemp)) {
                departmentDuplicate.add(mapTemp);
            }
            listMap.put(mapTemp, emailExcel);

            FileUtil.writeLogToFile(INFO, functionName, "END", configDTO.getPathFolderLog());
        } catch (Exception ex) {
            log.error("Can't read file excel: {}", ex.getMessage());
            FileUtil.writeLogToFile(ERROR, functionName, ex.getMessage(), configDTO.getPathFolderLog());
        } finally {
            try {
                if (wb != null) wb.close();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        return listMap;
    }

    public static boolean isRowEmpty(Row row) {
        boolean isEmpty = true;
        DataFormatter dataFormatter = new DataFormatter();
        if (row != null) {
            for (Cell cell : row) {
                if (dataFormatter.formatCellValue(cell).trim().length() > 0) {
                    isEmpty = false;
                    break;
                }
            }
        }
        return isEmpty;
    }

    public Map<String, List<User>> getAllUserByDepartment() {
        List<User> users = repository.getAll();
        Map<String, List<User>> listMap = new HashMap<>();
        for (User user : users) {
            if (user.getSecondaryDepartmentsAndRoles() != null) {
                user.getSecondaryDepartmentsAndRoles().forEach((key, value) -> {
                    if (listMap.containsKey(key)) {
                        List<User> list = listMap.get(key);
                        list.add(user);
                    } else {
                        List<User> list = new ArrayList<>();
                        list.add(user);
                        listMap.put(key, list);
                    }
                });
            }
        }
        return listMap;
    }

    public void responseData(List<Issue> success, List<Issue> fails) {
        try {
            APIResponseRedmine response = new APIResponseRedmine();
            URL url = new URL("http://10.116.41.47:3000/redmine");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            response.setSuccess(success);
            response.setFail(fails);
            ObjectMapper mapper = new ObjectMapper();
            String arrayToJson = mapper.writeValueAsString(response);
            OutputStream os = conn.getOutputStream();
            os.write(arrayToJson.getBytes());
            os.flush();
            new BufferedReader(new InputStreamReader((conn.getInputStream())));
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }
            conn.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateDepartmentToUser(String department, List<String> emails) {
        for (String email : emails) {
            User user = repository.getByEmail(email);
            Map<String, Set<UserGroup>> map;
            Set<UserGroup> userGroups = new HashSet<>();
            if (user.getSecondaryDepartmentsAndRoles() != null) {
                map = user.getSecondaryDepartmentsAndRoles();
                for (Map.Entry<String, Set<UserGroup>> entry : map.entrySet()) {
                    if (entry.getKey().equals(department)) {
                        userGroups = entry.getValue();
                    }
                }
            } else {
                map = new HashMap<>();
            }
            userGroups.add(UserGroup.USER);
            map.put(department, userGroups);
            user.setSecondaryDepartmentsAndRoles(map);
            repository.update(user);
        }
    }

    public void validateListEmailExistDB(Map<String, List<String>> mapList) {
        Set<String> setEmail = new HashSet<>();
        mapList.forEach((v, k) -> setEmail.addAll(k));
        for (String email : setEmail) {
            User user = repository.getByEmail(email);
            if (user == null) emailDoNotExist.add(email);
        }
    }

}