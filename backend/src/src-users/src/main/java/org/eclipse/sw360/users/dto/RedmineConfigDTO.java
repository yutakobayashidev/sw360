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
package org.eclipse.sw360.users.dto;

public class RedmineConfigDTO {
    private String pathFolder;
    private String pathFolderLog;
    private String urlApiRedmine;
    private String lastRunningTime;

    public RedmineConfigDTO() {
    }

    public RedmineConfigDTO(String pathFolder, String pathFolderLog, String urlApiRedmine, String lastRunningTime) {
        this.pathFolder = pathFolder;
        this.pathFolderLog = pathFolderLog;
        this.urlApiRedmine = urlApiRedmine;
        this.lastRunningTime = lastRunningTime;
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

    public String getLastRunningTime() {
        return lastRunningTime;
    }

    public void setLastRunningTime(String lastRunningTime) {
        this.lastRunningTime = lastRunningTime;
    }
}
