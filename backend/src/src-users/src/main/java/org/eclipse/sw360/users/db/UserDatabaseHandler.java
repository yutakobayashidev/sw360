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
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.*;
import org.apache.xmlbeans.impl.xb.xsdschema.Attribute;
import org.eclipse.sw360.datahandler.cloudantclient.DatabaseConnectorCloudant;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.couchdb.DatabaseConnector;
import org.eclipse.sw360.datahandler.db.UserRepository;
import org.eclipse.sw360.datahandler.db.UserSearchHandler;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftValidate;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.eclipse.sw360.users.dto.UserDTO;
import org.ektorp.http.HttpClient;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public UserDatabaseHandler(Supplier<CloudantClient> httpClient, String dbName) throws IOException {
        // Create the connector
        db = new DatabaseConnectorCloudant(httpClient, dbName);
        dbConnector = new DatabaseConnector(DatabaseSettings.getConfiguredHttpClient(), dbName);
        repository = new UserRepository(db);
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

    public void importFileToDB(String pathFolder) {
        try {
            List<User> users = repository.getAll();
            userDTOMap = new HashMap<>();
            for (User user : users) {
                UserDTO userDTO = UserDTO.convertToUserDTO(user);
                userDTOMap.put(user.getEmail(), userDTO);
            }
            Set<String> files = listFilesUsingFileWalk(pathFolder);
            for (String file : files) {
                checkFileFormat(pathFolder + "/" + file);
            }
            userDTOMap.forEach((key, value) -> {
                if (value.getId() == null || value.getId().isEmpty() || value.getId().equals("")) {
                    repository.add(value.convertToUser());
                } else {
                    repository.update(value.convertToUserUpdate());
                }
            });
        } catch (IOException e) {
            log.error("Can't read file: {}", e.getMessage());
        }
    }

    private static Set<String> listFilesUsingFileWalk(String dir) throws IOException {
        try (Stream<Path> stream = Files.walk(Paths.get(dir), 1)) {
            return stream
                    .filter(file -> !Files.isDirectory(file))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .collect(Collectors.toSet());
        }
    }

    private void checkFileFormat(String pathFile) {
        String extension = FilenameUtils.getExtension(pathFile);
        if (extension.equalsIgnoreCase("xlsx")) {
            readFileExcel(pathFile);
        } else {
            readFileCsv(pathFile);
        }
    }

    public void readFileCsv(String filePath) {
        try {
            File file = new File(filePath);
            CSVReader reader = new CSVReaderBuilder(new FileReader(file)).withSkipLines(1).build();
            List<String[]> rows = reader.readAll();
            String mapTemp = "";
            for (String[] row : rows) {
                if (row.length > 1) {
                    if (!Objects.equals(row[0], "")) mapTemp = row[0];
                    checkUser(row[1], mapTemp);
                }
            }
        } catch (IOException | CsvException e) {
            log.error("Can't read file csv: {}", e.getMessage());
        }
    }

    public void readFileExcel(String filePath) {
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
                    if (row.getCell(0) != null) mapTemp = row.getCell(0).getStringCellValue();
                    checkUser(row.getCell(1).getStringCellValue(), mapTemp);
                }
            }
        } catch (Exception ex) {
            log.error("Can't read file excel: {}", ex.getMessage());
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
            u.setEmail(email);
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

    public Map<String, List<User>> searchUsersByDepartment(String departmentKey) {
        Map<String, List<User>> listMap = getAllUserByDepartment();
        Map<String, List<User>> mapByDepartment = new HashMap<>();
        List<User> users = new ArrayList<>();

        for (Map.Entry<String, List<User>> entry : listMap.entrySet()) {
            if (entry.getKey().equals(departmentKey)) {
                users = entry.getValue();
                mapByDepartment.put(entry.getKey(), users);
            }
        }
        return mapByDepartment;
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

        List<String> emailsbyDepartment = getAllEmailByDepartment(departmentKey);
        List<String> emailByListUser = new ArrayList<>();
        List<User> users = repository.getAll();
        for (User user : users) {
            emailByListUser.add(user.getEmail());
        }
        List<String> emailOtherDepartment = new ArrayList<>(emailByListUser);
        emailOtherDepartment.removeAll(emailsbyDepartment);
        return emailOtherDepartment;
    }

    public void updateDepartmentToUser(String email, String department) {
        log.info("===========UpdateDepartmentToUser=========" + email + department);
        List<User> users = repository.getAll();
        User user = checkEmail(email);
        Map<String, Set<UserGroup>> map = new HashMap<>();
        map = user.getSecondaryDepartmentsAndRoles();
        Set<UserGroup> userGroups = new HashSet<>();
        for (Map.Entry<String, Set<UserGroup>> entry : map.entrySet()) {
            if (entry.getKey().equals(department)) {
                userGroups = entry.getValue();
            }
        }
        userGroups.add(UserGroup.USER);
        map.put(department, userGroups);
        user.setSecondaryDepartmentsAndRoles(map);
        repository.update(user);
    }

    private User checkEmail(String email) {
        List<User> users = repository.getAll();
        User user = new User();
        try {
            for (User userCheck : users) {
                if (userCheck.getEmail().equals(email)) {
                    return userCheck;
                }
            }
        } catch (NullPointerException ex) {
            log.error("Error Not foud User from Email!", ex);
        }
        return user;
    }

    public void updateDepartmentToListUser(List<String> emails, String department) {
        for (String email : emails) {
            updateDepartmentToUser(email, department);
        }
    }

    public void deleteDepartmentByEmail(String email, String departmentKey) {
        User user = checkEmail(email);
        Map<String, Set<UserGroup>> map = new HashMap<>();
        map = user.getSecondaryDepartmentsAndRoles();
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
}
