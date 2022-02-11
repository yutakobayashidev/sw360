package org.eclipse.sw360.users.dto;

import java.util.List;

public class ApiResponse {
    private List<Object> success;
    private List<Object> fail;

    public List<Object> getSuccess() {
        return success;
    }

    public void setSuccess(List<Object> success) {
        this.success = success;
    }

    public List<Object> getFail() {
        return fail;
    }

    public void setFail(List<Object> fail) {
        this.fail = fail;
    }
}
