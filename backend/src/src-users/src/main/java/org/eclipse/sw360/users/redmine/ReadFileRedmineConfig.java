package org.eclipse.sw360.users.redmine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.users.dto.RedmineConfigDTO;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
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
            if (i == 3) return (path + "config.json");
        }
        return (path + "config.json");
    }

    public RedmineConfigDTO readFileJson() {
        try {
            Reader reader = Files.newBufferedReader(Paths.get(getPathConfig()));
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(reader);
            JsonNode configRedmine = jsonNode.path("configRedmine");
            String username = configRedmine.path("username").asText();
            String password = configRedmine.path("password").asText();
            String url = configRedmine.path("url").asText();
            Long projectId = configRedmine.path("projectId").asLong();
            Long trackerId = configRedmine.path("trackerId").asLong();
            Long statusNameOpenId = configRedmine.path("statusNameOpenId").asLong();
            Long statusNameClosedId = configRedmine.path("statusNameClosedId").asLong();
            String pathFolder = configRedmine.path("pathFolder").asText();
            String pathFolderLog = pathFolder + FOLDER_LOG;
            return new RedmineConfigDTO(username, password, url, projectId, trackerId, statusNameOpenId, statusNameClosedId, pathFolder, pathFolderLog);
        } catch (IOException e) {
            log.error("An I/O error occurred: {}", e.getMessage());
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
            map.put("username", redmineConfigDTO.getUsername());
            map.put("password", redmineConfigDTO.getPassword());
            map.put("url", redmineConfigDTO.getUrl());
            map.put("projectId", redmineConfigDTO.getProjectId());
            map.put("trackerId", redmineConfigDTO.getTrackerId());
            map.put("statusNameOpenId", redmineConfigDTO.getStatusNameOpenId());
            map.put("statusNameClosedId", redmineConfigDTO.getStatusNameClosedId());
            map.put("pathFolder", pathFolder);
            configRedmine.put("configRedmine", map);
            ObjectMapper mapper = new ObjectMapper();
            writer.write(mapper.writeValueAsString(configRedmine));
        } catch (IOException e) {
            log.error("An I/O error occurred: {}", e.getMessage());
        } finally {
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
