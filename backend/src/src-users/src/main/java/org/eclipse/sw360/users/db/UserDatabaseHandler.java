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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import org.eclipse.sw360.users.dto.DepartmentConfigDTO;
import org.eclipse.sw360.users.redmine.ReadFileDepartmentConfig;
import org.eclipse.sw360.users.util.FileUtil;
import org.ektorp.http.HttpClient;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
    private static final Logger log = LogManager.getLogger(UserDatabaseHandler.class);
    private ReadFileDepartmentConfig readFileRedmineConfig;
    private static final String SUCCESS = "SUCCESS";
    private static final String FAIL = "FAIL";
    private static final String TITLE = "IMPORT";
    private static boolean IMPORT_DEPARTMENT_STATUS = false;
    private List<String> departmentDuplicate;
    private List<String> emailDoNotExist;
    DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

    public UserDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName) throws IOException {
        // Create the connector
        db = new DatabaseConnectorCloudant(httpClient, dbName);
        dbConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), dbName);
        repository = new UserRepository(db);
        readFileRedmineConfig = new ReadFileDepartmentConfig();
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
        return repository.getByEmail(email);
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
        departmentDuplicate = new ArrayList<>();
        emailDoNotExist = new ArrayList<>();
        List<String> listFileSuccess = new ArrayList<>();
        List<String> listFileFail = new ArrayList<>();
        RequestSummary requestSummary = new RequestSummary().setTotalAffectedElements(0).setMessage("");
        DepartmentConfigDTO configDTO = readFileRedmineConfig.readFileJson();
        String pathFolderLog = configDTO.getPathFolderLog();
        Map<String, List<String>> mapArrayList = new HashMap<>();
        if (IMPORT_DEPARTMENT_STATUS) {
            return requestSummary.setRequestStatus(RequestStatus.PROCESSING);
        }
        IMPORT_DEPARTMENT_STATUS = true;
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        String lastRunningTime = dateFormat.format(calendar.getTime());
        readFileRedmineConfig.writeLastRunningTimeConfig(lastRunningTime);
        try {
            FileUtil.writeLogToFile("", "START IMPORT DEPARTMENT", "", pathFolderLog);
            Set<String> files = FileUtil.listPathFiles(pathFolder);
            for (String file : files) {
                String extension = FilenameUtils.getExtension(file);
                if (extension.equalsIgnoreCase("xlsx") || extension.equalsIgnoreCase("xls")) {
                    mapArrayList = readFileExcel(file);
                } else if (extension.equalsIgnoreCase("csv")) {
                    mapArrayList = readFileCsv(file);
                }
                Map<String, User> mapEmail = validateListEmailExistDB(mapArrayList);
                String fileName = FilenameUtils.getName(file);
                if (departmentDuplicate.isEmpty() && emailDoNotExist.isEmpty()) {
                    mapArrayList.forEach((k, v) -> v.forEach(email -> updateDepartmentToUser(mapEmail.get(email), k)));
                    String joined = mapArrayList.keySet().stream().sorted().collect(Collectors.joining(", "));
                    listFileSuccess.add(fileName);
                    FileUtil.writeLogToFile(TITLE, "DEPARTMENT [" + joined + "] - FILE NAME: [" + fileName + "]", SUCCESS, pathFolderLog);
                } else {
                    if (!departmentDuplicate.isEmpty()) {
                        List<String> departmentDuplicateOrder = departmentDuplicate.stream().sorted().collect(Collectors.toList());
                        String joined = String.join(", ", departmentDuplicateOrder);
                        FileUtil.writeLogToFile(TITLE, "DEPARTMENT [" + joined + "] IS DUPLICATE - FILE NAME: [" + fileName + "]", FAIL, pathFolderLog);
                        departmentDuplicate = new ArrayList<>();
                    }
                    if (!emailDoNotExist.isEmpty()) {
                        List<String> emailDoNotExistOrder = emailDoNotExist.stream().sorted().collect(Collectors.toList());
                        String joined = String.join(", ", emailDoNotExistOrder);
                        FileUtil.writeLogToFile(TITLE, "USER [" + joined + "] DOES NOT EXIST - FILE NAME: [" + fileName + "]", FAIL, pathFolderLog);
                        emailDoNotExist = new ArrayList<>();
                    }
                    listFileFail.add(fileName);
                }
            }
            IMPORT_DEPARTMENT_STATUS = false;
            requestSummary.setTotalAffectedElements(listFileSuccess.size());
            requestSummary.setTotalElements(listFileSuccess.size() + listFileFail.size());
            requestSummary.setRequestStatus(RequestStatus.SUCCESS);
        } catch (Exception e) {
            IMPORT_DEPARTMENT_STATUS = false;
            String msg = "Failed to import department";
            requestSummary.setMessage(msg);
            requestSummary.setRequestStatus(RequestStatus.FAILURE);
            FileUtil.writeLogToFile(TITLE, "FILE ERROR: " + e.getMessage(), "", pathFolderLog);
        }
        FileUtil.writeLogToFile(TITLE, "[ FILE SUCCESS: " + listFileSuccess.size() + " - " + "FILE FAIL: " + listFileFail.size() + " - " + "TOTAL FILE: " + (listFileSuccess.size() + listFileFail.size()) + " ]", "Complete The File Import", pathFolderLog);
        FileUtil.writeLogToFile(TITLE, "END IMPORT DEPARTMENT", "", pathFolderLog);

        return requestSummary;
    }

    public Map<String, List<String>> readFileCsv(String filePath) {
        Map<String, List<String>> listMap = new HashMap<>();
        List<String> emailCsv = new ArrayList<>();
        try {
            File file = new File(filePath);
            CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(1).build();
            List<String[]> rows = reader.readAll();
            String mapTemp = "";
            for (String[] row : rows) {
                if (row.length > 1) {
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
        } catch (IOException | CsvException e) {
            log.error("Can't read file csv: {}", e.getMessage());
        }
        return listMap;
    }

    public Map<String, List<String>> readFileExcel(String filePath) {
        Map<String, List<String>> listMap = new HashMap<>();
        List<String> emailExcel = new ArrayList<>();

        Workbook wb = null;
        try (InputStream inp = new FileInputStream(filePath)) {
            wb = WorkbookFactory.create(inp);
            Sheet sheet = wb.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();
            rows.next();
            String mapTemp = "";
            while (rows.hasNext()) {
                Row row = rows.next();
                if (!isRowEmpty(row)) {
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
        } catch (Exception ex) {
            log.error("Can't read file excel: {}", ex.getMessage());
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


    public Map<String, List<User>> searchUsersByDepartment(String departmentKey) {
        Map<String, List<User>> listMap = getAllUserByDepartment();
        Map<String, List<User>> mapByDepartment = new HashMap<>();
        List<User> users;
        for (Map.Entry<String, List<User>> entry : listMap.entrySet()) {
            if (entry.getKey().equals(departmentKey)) {
                users = entry.getValue();
                mapByDepartment.put(entry.getKey(), users);
            }
        }
        return mapByDepartment;
    }

    public String searchUsersByDepartmentToJson(String departmentKey) {
        Map<String, List<User>> listMap = searchUsersByDepartment(departmentKey);
        JsonArray departmentJsonArray = new JsonArray();
        List<User> userList = new ArrayList<>();
        for (Map.Entry<String, List<User>> entry : listMap.entrySet()) {
            if (entry.getKey().equals(departmentKey)) {
                userList = entry.getValue();
            }
        }
        for (User user : userList) {
            JsonObject object = new JsonObject();
            object.addProperty("email", user.getEmail());
            departmentJsonArray.add(object);
        }
        return departmentJsonArray.toString().replace("\\", "");
    }

    public List<String> getAllDepartment() {
        Map<String, List<User>> listMap = getAllUserByDepartment();
        List<String> departments = new ArrayList<>();
        for (Map.Entry<String, List<User>> entry : listMap.entrySet()) {
            departments.add(entry.getKey());
        }
        return departments;
    }

    public List<String> getAllEmailByDepartment(String departmentKey) {
        Map<String, List<User>> listMap = getAllUserByDepartment();
        List<String> emails = new ArrayList<>();
        List<User> users = new ArrayList<>();
        for (Map.Entry<String, List<User>> entry : listMap.entrySet()) {
            if (entry.getKey().equals(departmentKey)) {
                users = entry.getValue();
            }
        }
        for (User user : users) {
            emails.add(user.getEmail());
        }
        return emails;
    }

    public List<String> getAllEmailOtherDepartment(String departmentKey) {
        List<String> emailsByDepartment = getAllEmailByDepartment(departmentKey);
        List<String> emailByListUser = new ArrayList<>();
        List<User> users = repository.getAll();
        for (User user : users) {
            emailByListUser.add(user.getEmail());
        }
        List<String> emailOtherDepartment = new ArrayList<>(emailByListUser);
        emailOtherDepartment.removeAll(emailsByDepartment);
        return emailOtherDepartment;
    }

    public String getAllEmailOtherDepartmentToJson(String departmentKey) {
        JsonArray emailJsonArray = new JsonArray();
        List<String> emailOtherDepartment = getAllEmailOtherDepartment(departmentKey);
        for (String email : emailOtherDepartment) {
            JsonObject object = new JsonObject();
            object.addProperty("email", email);
            emailJsonArray.add(object);
        }
        return emailJsonArray.toString().replace("\\", "");
    }

    public Map<String, User> validateListEmailExistDB(Map<String, List<String>> mapList) {
        Map<String, User> listUser = new HashMap<>();
        Set<String> setEmail = new HashSet<>();
        mapList.forEach((v, k) -> setEmail.addAll(k));
        for (String email : setEmail) {
            User user = repository.getByEmail(email);
            if (user == null) {
                emailDoNotExist.add(email);
            } else {
                listUser.put(email, user);
            }
        }
        return listUser;
    }

    public void updateDepartmentToUser(User user, String department) {
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

    public void updateDepartmentToListUser(List<User> users, String department) {
        List<User> usersByDepartment = getAllUsersByDepartment(department);
        deleteDepartmentByListUser(usersByDepartment, department);
        users.forEach(u -> {
            User user = repository.getByEmail(u.getEmail());
            updateDepartmentToUser(user, department);
        });
    }

    public void deleteDepartmentByUser(User user, String departmentKey) {
        Map<String, Set<UserGroup>> map = user.getSecondaryDepartmentsAndRoles();
        Set<UserGroup> userGroups = new HashSet<>();
        for (Map.Entry<String, Set<UserGroup>> entry : map.entrySet()) {
            if (entry.getKey().equals(departmentKey)) {
                userGroups = entry.getValue();
            }
        }
        map.remove(departmentKey, userGroups);
        user.setSecondaryDepartmentsAndRoles(map);
        repository.update(user);
    }

    public void deleteUserByDepartment(String department) {
        List<User> usersByDepartment = getAllUsersByDepartment(department);
        deleteDepartmentByListUser(usersByDepartment, department);
    }

    public void deleteDepartmentByListUser(List<User> users, String departmentKey) {
        for (User user : users) {
            deleteDepartmentByUser(user, departmentKey);
        }
    }

    public List<User> getAllUserByListEmail(List<String> emails) {
        List<User> users = new ArrayList<>();
        for (String email : emails) {
            if (getByEmail(email) != null) users.add(getByEmail(email));
        }
        return users;
    }

    public List<User> getAllUsersByDepartment(String departmentKey) {
        Map<String, List<User>> listMap = getAllUserByDepartment();
        List<User> users = new ArrayList<>();
        for (Map.Entry<String, List<User>> entry : listMap.entrySet()) {
            if (entry.getKey().equals(departmentKey)) {
                users = entry.getValue();
            }
        }
        return users;
    }
}

