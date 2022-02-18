/*
 * Copyright TOSHIBA CORPORATION, 2022. Part of the SW360 Portal Project.
 * Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2022. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.users.redmine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.users.dto.RedmineConfigDTO;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ReadFileRedmineConfig {

    private static final Logger log = LogManager.getLogger(ReadFileRedmineConfig.class);
    private static final String FOLDER_LOG = "/logs/";

    protected String getPathConfig() throws IOException {
        StringBuilder path = new StringBuilder("/");
        File file = File.createTempFile("check", "text");
        String pathFile = file.getPath();
        String[] parts = pathFile.split("/");
        for (int i = 0; i < parts.length; i++) {
            path.append(parts[i + 1]).append("/");
            if (i == 3) return (path + "department-config.json");
        }
        return (path + "department-config.json");
    }

    public RedmineConfigDTO readFileJson() {
        try {
            File file = new File(getPathConfig());
            if (file.exists()) {
                Reader reader = Files.newBufferedReader(Paths.get(getPathConfig()));
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonNode = objectMapper.readTree(reader);
                JsonNode configRedmine = jsonNode.path("configRedmine");
                String pathFolder = configRedmine.path("pathFolder").asText();
                String urlApiRedmine = configRedmine.path("urlApiRedmine").asText();
                String pathFolderLog = "";
                if (!pathFolder.isEmpty()) pathFolderLog = pathFolder + FOLDER_LOG;
                return new RedmineConfigDTO(pathFolder, pathFolderLog, urlApiRedmine);
            }
        } catch (FileNotFoundException e) {
            log.error("Error not find the file: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Unread file error: {}", e.getMessage());
        }
        return null;
    }

    public void writePathFolderConfig(String pathFolder) {
        RedmineConfigDTO redmineConfigDTO = readFileJson();
        BufferedWriter writer = null;
        try {
            writer = Files.newBufferedWriter(Paths.get(getPathConfig()));
            Map<String, Object> configRedmine = new HashMap<>();
            Map<String, Object> map = new HashMap<>();
            map.put("pathFolder", pathFolder);
            map.put("urlApiRedmine", redmineConfigDTO.getUrlApiRedmine());
            configRedmine.put("configRedmine", map);
            ObjectMapper mapper = new ObjectMapper();
            writer.write(mapper.writeValueAsString(configRedmine));
        } catch (FileNotFoundException e) {
            log.error("Error not find the file: {}", e.getMessage());
        } catch (IOException e) {
            log.error("Unread file error: {}", e.getMessage());
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
