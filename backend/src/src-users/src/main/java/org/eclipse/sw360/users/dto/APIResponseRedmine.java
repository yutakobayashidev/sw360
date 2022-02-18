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

import java.util.List;

public class APIResponseRedmine {
    private List<Issue> success;
    private List<Issue> fail;

    public List<Issue> getSuccess() {
        return success;
    }

    public void setSuccess(List<Issue> success) {
        this.success = success;
    }

    public List<Issue> getFail() {
        return fail;
    }

    public void setFail(List<Issue> fail) {
        this.fail = fail;
    }
}
