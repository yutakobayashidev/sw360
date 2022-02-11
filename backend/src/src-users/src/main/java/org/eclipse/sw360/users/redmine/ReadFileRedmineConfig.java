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
            if (i == 3) return (path + "path-folder-config.json");
        }
        return (path + "path-folder-config.json");
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
                String pathFolderLog = pathFolder + FOLDER_LOG;
                return new RedmineConfigDTO(pathFolder, pathFolderLog);
            }
        } catch (IOException e) {
            log.error("An I/O error occurred: {}", e.getMessage());
        }
        return null;
    }

    public void writePathFolderConfig(String pathFolder) {
        BufferedWriter writer = null;
        try {
            writer = Files.newBufferedWriter(Paths.get(getPathConfig()));
            Map<String, Object> configRedmine = new HashMap<>();
            Map<String, Object> map = new HashMap<>();
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
