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
