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

public class DepartmentConfigDTO {
    private String pathFolder;
    private String pathFolderLog;
    private String lastRunningTime;
    private int showFileLogFrom;

    public DepartmentConfigDTO() {
    }

    public DepartmentConfigDTO(String pathFolder, String pathFolderLog, String lastRunningTime, int showFileLogFrom) {
        this.pathFolder = pathFolder;
        this.pathFolderLog = pathFolderLog;
        this.lastRunningTime = lastRunningTime;
        this.showFileLogFrom = showFileLogFrom;
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

    public String getLastRunningTime() {
        return lastRunningTime;
    }

    public void setLastRunningTime(String lastRunningTime) {
        this.lastRunningTime = lastRunningTime;
    }

    public int getShowFileLogFrom() {
        return showFileLogFrom;
    }

    public void setShowFileLogFrom(int showFileLogFrom) {
        this.showFileLogFrom = showFileLogFrom;
    }
}
