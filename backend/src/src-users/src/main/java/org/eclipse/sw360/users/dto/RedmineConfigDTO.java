package org.eclipse.sw360.users.dto;

public class RedmineConfigDTO {
    private String username;
    private String password;
    private String url;
    private Long projectId;
    private Long trackerId;
    private Long statusNameOpenId;
    private Long statusNameClosedId;
    private String pathFolder;
    private String pathFolderLog;

    public RedmineConfigDTO() {
    }

    public RedmineConfigDTO(String username, String password, String url, Long projectId, Long trackerId, Long statusNameOpenId, Long statusNameClosedId, String pathFolder, String pathFolderLog) {
        this.username = username;
        this.password = password;
        this.url = url;
        this.projectId = projectId;
        this.trackerId = trackerId;
        this.statusNameOpenId = statusNameOpenId;
        this.statusNameClosedId = statusNameClosedId;
        this.pathFolder = pathFolder;
        this.pathFolderLog = pathFolderLog;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public Long getTrackerId() {
        return trackerId;
    }

    public void setTrackerId(Long trackerId) {
        this.trackerId = trackerId;
    }

    public Long getStatusNameOpenId() {
        return statusNameOpenId;
    }

    public void setStatusNameOpenId(Long statusNameOpenId) {
        this.statusNameOpenId = statusNameOpenId;
    }

    public Long getStatusNameClosedId() {
        return statusNameClosedId;
    }

    public void setStatusNameClosedId(Long statusNameClosedId) {
        this.statusNameClosedId = statusNameClosedId;
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
