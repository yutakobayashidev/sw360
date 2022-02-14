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
import org.eclipse.sw360.users.dto.ApiResponse;
import org.eclipse.sw360.users.dto.Issue;
import org.eclipse.sw360.users.dto.RedmineConfigDTO;
import org.eclipse.sw360.users.dto.UserDTO;
import org.eclipse.sw360.users.redmine.ReadFileRedmineConfig;
import org.eclipse.sw360.users.util.FileUtil;
import org.ektorp.http.HttpClient;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
    private Map<String, UserDTO> userDTOMap;
    private static final Logger log = LogManager.getLogger(UserDatabaseHandler.class);
    private static final String DEPARTMENT = "DEPARTMENT";
    private ReadFileRedmineConfig readFileRedmineConfig;
    private static final String INFO = "INFO";
    private static final String ERROR = "ERROR";
    private static boolean IMPORT_STATUS = false;
    private List<Object> listIssueSuccess = new ArrayList<>();
    private List<Object> listIssueFail = new ArrayList<>();

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
        RequestSummary requestSummary = new RequestSummary().setTotalAffectedElements(0).setMessage("");
        RedmineConfigDTO configDTO = readFileRedmineConfig.readFileJson();
        if (IMPORT_STATUS) {
            return requestSummary.setRequestStatus(RequestStatus.PROCESSING);
        }
        log.info("****************1. RequestSummary***********"+IMPORT_STATUS);
        IMPORT_STATUS = true;
        log.info("****************2. RequestSummary***********"+IMPORT_STATUS);
        try {
            FileUtil.writeErrorToFile(INFO, functionName, "START", configDTO.getPathFolderLog());
            Set<String> files = FileUtil.listFilesUsingFileWalk(pathFolder);
            for (String file : files) {
                checkFileFormat(pathFolder + "/" + file);
            }
            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
            IMPORT_STATUS = false;
            FileUtil.writeErrorToFile(INFO, functionName, "END", configDTO.getPathFolderLog());
        } catch (IOException e) {
            IMPORT_STATUS = false;
            FileUtil.writeErrorToFile(ERROR, functionName, e.getMessage(), configDTO.getPathFolderLog());
            String msg = "Failed to import department";
            log.error("Can't read file: {}", e.getMessage());
            requestSummary.setMessage(msg);
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
        }
        log.info("****************3. RequestSummary***********"+IMPORT_STATUS);
        responseData(listIssueSuccess, listIssueFail);
        return requestSummary;
    }

    private void checkFileFormat(String pathFile) {
        String extension = FilenameUtils.getExtension(pathFile);
        if (extension.equalsIgnoreCase("xlsx") || extension.equalsIgnoreCase("xls")) {
            userDTOMap = new HashMap<>();
            listIssueSuccess = new ArrayList<>();
            listIssueFail = new ArrayList<>();
            File file = new File(pathFile);
            List<User> users = repository.getAll();
            userDTOMap = new HashMap<>();
            for (User user : users) {
                UserDTO userDTO = UserDTO.convertToUserDTO(user);
                userDTOMap.put(user.getEmail(), userDTO);
            }
            readFileExcel(pathFile);
            HashMap<String, Boolean> resMap = new HashMap<>();
            userDTOMap.forEach((key, value) -> {
                if (value.getId() == null || value.getId().isEmpty() || value.getId().equals("")) {
                    User user = value.convertToUser();
                    boolean res = repository.add(user);
                    user.getSecondaryDepartmentsAndRoles().forEach((keyD, valueD) -> {
                        if (res) {
                            if (!resMap.containsKey(keyD)) {
                                resMap.put(keyD, true);
                            }
                        } else {
                            resMap.put(keyD, false);
                        }
                    });
                } else {
                    try {
                        User user = value.convertToUserUpdate();
                        repository.update(user);
                        user.getSecondaryDepartmentsAndRoles().forEach((keyD, valueD) -> {
                            if (!resMap.containsKey(keyD)) {
                                resMap.put(keyD, true);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            Issue issueSuccess = new Issue();
            Issue issueFail = new Issue();
            String descriptionSuccess = resMap.entrySet().stream()
                    .filter(x -> Boolean.TRUE.equals(x.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(", "));
            String issueId = pathFile.substring(pathFile.lastIndexOf("_") + 1, pathFile.lastIndexOf("."));
            String fileName = file.getName().replace(pathFile.substring(pathFile.lastIndexOf("_"), pathFile.lastIndexOf(".")), "");
            if (!issueSuccess.equals("") && !descriptionSuccess.isEmpty()) {
                issueSuccess.setIssue_id(issueId);
                issueSuccess.setDescription("Department [" + descriptionSuccess + "] added successfully - File name [" + fileName + "]");
                listIssueSuccess.add(issueSuccess);
            }
            String descriptionFail = resMap.entrySet().stream()
                    .filter(x -> !Boolean.TRUE.equals(x.getValue()))
                    .map(Map.Entry::getKey)
                    .collect(Collectors.joining(", "));
            if (!issueFail.equals("") && !descriptionFail.isEmpty()) {
                issueFail.setIssue_id(issueId);
                issueFail.setDescription("Department [" + descriptionFail + "] - File name [" + fileName + "]");
                listIssueFail.add(issueFail);
            }
        } else if (extension.equalsIgnoreCase("csv")) {
            readFileCsv(pathFile);

        }
    }

    public void readFileCsv(String filePath) {
        String functionName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        RedmineConfigDTO configDTO = readFileRedmineConfig.readFileJson();
        FileUtil.writeErrorToFile(INFO, functionName, "START", configDTO.getPathFolderLog());
        try {
            File file = new File(filePath);
            FileUtil.writeErrorToFile(INFO, functionName, file.getName(), configDTO.getPathFolderLog());
            CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(1).build();
            List<String[]> rows = reader.readAll();
            String mapTemp = "";
            for (String[] row : rows) {
                if (row.length > 1) {
                    if (!Objects.equals(row[0], "")) mapTemp = row[0];
                    checkUser(row[1], mapTemp);
                }
            }
            FileUtil.writeErrorToFile(INFO, functionName, "END", configDTO.getPathFolderLog());
        } catch (IOException | CsvException e) {
            log.error("Can't read file csv: {}", e.getMessage());
            FileUtil.writeErrorToFile(ERROR, functionName, e.getMessage(), configDTO.getPathFolderLog());
        }
    }

    public void readFileExcel(String filePath) {
        String functionName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        RedmineConfigDTO configDTO = readFileRedmineConfig.readFileJson();
        Workbook wb = null;
        FileUtil.writeErrorToFile(INFO, functionName, "START", configDTO.getPathFolderLog());
        try (InputStream inp = new FileInputStream(filePath)) {
            FileUtil.writeErrorToFile(INFO, functionName, FilenameUtils.getName(filePath), configDTO.getPathFolderLog());
            wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            rows.next();
            String mapTemp = "";
            while (rows.hasNext()) {
                Row row = rows.next();
                if (!isRowEmpty(row)) {
                    if (row.getCell(0) != null) mapTemp = row.getCell(0).getStringCellValue();
                    checkUser(row.getCell(1).getStringCellValue(), mapTemp);
                }
            }
            FileUtil.writeErrorToFile(INFO, functionName, "END", configDTO.getPathFolderLog());
        } catch (Exception ex) {
            log.error("Can't read file excel: {}", ex.getMessage());
            FileUtil.writeErrorToFile(ERROR, functionName, ex.getMessage(), configDTO.getPathFolderLog());
        } finally {
            try {
                if (wb != null) wb.close();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
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

    private void checkUser(String email, String apartment) {
        UserDTO userDTO = userDTOMap.get(email);
        if (userDTO == null) {
            UserDTO u = new UserDTO();
            u.setEmail(email)
            ;
            u.setDepartment(DEPARTMENT);
            Set<UserGroup> userGroups = new HashSet<>();
            userGroups.add(UserGroup.USER);
            Map<String, Set<UserGroup>> map = new HashMap<>();
            map.put(apartment, userGroups);
            u.setSecondaryDepartmentsAndRoles(map);
            userDTOMap.put(email, u);
        } else {
            if (!userDTO.getSecondaryDepartmentsAndRoles().containsKey(apartment)) {
                Set<UserGroup> userGroups = new HashSet<>();
                userGroups.add(UserGroup.USER);
                userDTO.getSecondaryDepartmentsAndRoles().put(apartment, userGroups);
            }
        }
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

    public void responseData(List<Object> success, List<Object> fails) {
        try {
            ApiResponse response = new ApiResponse();
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

}