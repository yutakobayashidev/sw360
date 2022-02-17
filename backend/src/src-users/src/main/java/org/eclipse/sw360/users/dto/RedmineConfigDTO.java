package org.eclipse.sw360.users.dto;

public class RedmineConfigDTO {
    private String pathFolder;
    private String pathFolderLog;

    public RedmineConfigDTO() {
    }

    public RedmineConfigDTO(String pathFolder, String pathFolderLog) {
        this.pathFolder = pathFolder;
        this.pathFolderLog = pathFolderLog;
    }

    public String getPathFolder() {
        return pathFolder;
    }

    public void setPathFolder(String pathFolder) {
        this.pathFolder = pathFolder;
    }

    public String getPathFolderLog() {
        return pathFolderLog;
    }

    public void setPathFolderLog(String pathFolderLog) {
        this.pathFolderLog = pathFolderLog;
    }
}
