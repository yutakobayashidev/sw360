package org.eclipse.sw360.users.dto;

import java.util.List;

public class ApiResponse {
    private List<Object> success;
    private List<Object> fails;

    public List<Object> getSuccess() {
        return success;
    }

    public void setSuccess(List<Object> success) {
        this.success = success;
    }

    public List<Object> getFails() {
        return fails;
    }

    public void setFails(List<Object> fails) {
        this.fails = fails;
    }
}
