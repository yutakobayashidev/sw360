package org.eclipse.sw360.users.dto;

public class RedmineConfigDTO {
    private String pathFolder;
    private String pathFolderLog;
    private String urlApiRedmine;

    public RedmineConfigDTO() {
    }

    public RedmineConfigDTO(String pathFolder, String pathFolderLog, String urlApiRedmine) {
        this.pathFolder = pathFolder;
        this.pathFolderLog = pathFolderLog;
        this.urlApiRedmine = urlApiRedmine;
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

    public String getUrlApiRedmine() {
        return urlApiRedmine;
    }

    public void setUrlApiRedmine(String urlApiRedmine) {
        this.urlApiRedmine = urlApiRedmine;
    }
}
