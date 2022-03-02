/*
 * Copyright Siemens AG, 2013-2015. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users;

import com.cloudant.client.api.CloudantClient;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.DatabaseSettings;
import org.eclipse.sw360.datahandler.thrift.PaginationData;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.RequestSummary;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.users.db.UserDatabaseHandler;
import org.eclipse.sw360.users.dto.DepartmentConfigDTO;
import org.eclipse.sw360.users.util.FileUtil;
import org.eclipse.sw360.users.util.ReadFileDepartmentConfig;
import org.ektorp.http.HttpClient;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Assert.assertNotNull;

/**
 * Implementation of the Thrift service
 *
 * @author cedric.bodet@tngtech.com
 */
public class UserHandler implements UserService.Iface {

    private static final Logger log = LogManager.getLogger(UserHandler.class);
    private static final String EXTENSION = ".log";

    private UserDatabaseHandler db;
    private ReadFileDepartmentConfig readFileRedmineConfig;

    public UserHandler() throws IOException {
        db = new UserDatabaseHandler(DatabaseSettings.getConfiguredClient(), DatabaseSettings.COUCH_DB_USERS);
        readFileRedmineConfig = new ReadFileDepartmentConfig();
    }

    public UserHandler(Supplier<CloudantClient> client, Supplier<HttpClient> httpclient, String userDbName) throws IOException {
        db = new UserDatabaseHandler(client, httpclient, userDbName);
    }

    @Override
    public User getUser(String id) {
        return db.getUser(id);
    }

    @Override
    public User getByEmail(String email) throws TException {
        StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[2];
        assertNotEmpty(email, "Invalid empty email " + stackTraceElement.getFileName() + ": " + stackTraceElement.getLineNumber());

        if (log.isTraceEnabled()) log.trace("getByEmail: " + email);

        return db.getByEmail(email);
    }

    @Override
    public User getByEmailOrExternalId(String email, String externalId) throws TException {
        User user = getByEmail(email);
        if (user == null) {
            user = db.getByExternalId(externalId);
        }
        if (user != null && user.isDeactivated()) {
            return null;
        }
        return user;
    }

    @Override
    public User getByApiToken(String token) throws TException {
        assertNotEmpty(token);
        return db.getByApiToken(token);
    }

    @Override
    public List<User> searchUsers(String searchText) {
        return db.searchUsers(searchText);
    }

    @Override
    public List<User> getAllUsers() {
        return db.getAll();
    }

    @Override
    public RequestStatus addUser(User user) throws TException {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.addUser(user);
    }

    @Override
    public RequestStatus updateUser(User user) throws TException {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.updateUser(user);
    }

    @Override
    public RequestStatus deleteUser(User user, User adminUser) throws TException {
        assertNotNull(user);
        assertNotNull(user.getEmail());
        return db.deleteUser(user, adminUser);
    }

    @Override
    public String getDepartmentByEmail(String email) throws TException {
        User user = getByEmail(email);
        return user != null ? user.getDepartment() : null;
    }

    @Override
    public Map<PaginationData, List<User>> getUsersWithPagination(User user, PaginationData pageData)
            throws TException {
        return db.getUsersWithPagination(pageData);
    }

    @Override
    public List<User> refineSearch(String text, Map<String, Set<String>> subQueryRestrictions) throws TException {
        return db.search(text, subQueryRestrictions);
    }

    @Override
    public Set<String> getUserDepartments() throws TException {
        return db.getUserDepartments();
    }

    @Override
    public Set<String> getAllEmailsByDepartmentKey(String departmentName) throws TException {
        return db.getAllEmailsByDepartmentKey(departmentName);
    }

    public Set<String> getUserEmails() throws TException {
        return db.getUserEmails();
    }


    @Override
    public RequestSummary importFileToDB() {
        DepartmentConfigDTO configDTO = readFileRedmineConfig.readFileJson();
        RequestSummary requestSummary = new RequestSummary();
        if (!configDTO.getPathFolder().isEmpty()) {
            requestSummary = db.importFileToDB(configDTO.getPathFolder());
        }
        return requestSummary;
    }

    @Override
    public RequestStatus importDepartmentSchedule() throws TException {
        DepartmentConfigDTO configDTO = readFileRedmineConfig.readFileJson();
        db.importFileToDB(configDTO.getPathFolder());
        return RequestStatus.SUCCESS;
    }

    @Override
    public Map<String, List<User>> getAllUserByDepartment() throws TException {
        return db.getAllUserByDepartment();
    }

    @Override
    public String searchUsersByDepartmentToJson(String department) throws TException {
        return db.searchUsersByDepartmentToJson(department);
    }

    @Override
    public String getAllEmailOtherDepartmentToJson(String department) throws TException {
        return db.getAllEmailOtherDepartmentToJson(department);
    }

    @Override
    public Set<String> getListFileLog() {
        try {
            DepartmentConfigDTO configDTO = readFileRedmineConfig.readFileJson();
            if (configDTO != null && !configDTO.getPathFolderLog().isEmpty()) {
                String path = configDTO.getPathFolderLog();
                File theDir = new File(path);
                if (!theDir.exists()) theDir.mkdirs();
                return FileUtil.listFileNames(path);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptySet();
    }

    @Override
    public Map<String, List<String>> getAllContentFileLog() {
        Map<String, List<String>> listMap = new HashMap<>();
        try {
            DepartmentConfigDTO configDTO = readFileRedmineConfig.readFileJson();
            if (configDTO != null && configDTO.getPathFolderLog().length() > 0) {
                String path = configDTO.getPathFolderLog();
                File theDir = new File(path);
                if (!theDir.exists()) theDir.mkdirs();
                Set<String> fileNamesSet = FileUtil.getListFilesOlderThanNDays(configDTO.getShowFileLogFrom(), path);
                for (String fileName : fileNamesSet) {
                    listMap.put(FilenameUtils.getName(fileName).replace(EXTENSION, ""), FileUtil.readFileLog(fileName));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return listMap;
    }

    @Override
    public String getLastModifiedFileName() throws TException {
        try {
            DepartmentConfigDTO configDTO = readFileRedmineConfig.readFileJson();
            if (configDTO != null && !configDTO.getPathFolderLog().isEmpty()) {
                String path = configDTO.getPathFolderLog();
                File theDir = new File(path);
                if (!theDir.exists()) theDir.mkdirs();
                Set<String> strings = FileUtil.listFileNames(path);
                if (!strings.isEmpty()) {
                    File file = FileUtil.getFileLastModified(path);
                    return file.getName().replace(EXTENSION, "");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public String getPathConfigDepartment() throws TException {
        try {
            DepartmentConfigDTO configDTO = readFileRedmineConfig.readFileJson();
            if (configDTO != null && !configDTO.getPathFolder().isEmpty()) {
                return configDTO.getPathFolder();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void writePathFolderConfig(String pathFolder) throws TException {
        readFileRedmineConfig.writePathFolderConfig(pathFolder);
    }

    @Override
    public String getLastRunningTime() throws TException {
        try {
            DepartmentConfigDTO configDTO = readFileRedmineConfig.readFileJson();
            if (configDTO != null && !configDTO.getLastRunningTime().isEmpty()) {
                return configDTO.getLastRunningTime();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    public void updateDepartmentToListUser(List<User> users, String department) throws TException {
        db.updateDepartmentToListUser(users, department);
    }

    @Override
    public void deleteDepartmentByListUser(List<User> users, String department) throws TException {
        db.deleteDepartmentByListUser(users, department);
    }

    @Override
    public List<User> getAllUserByListEmail(List<String> emails) throws TException {
        return db.getAllUserByListEmail(emails);
    }

}
