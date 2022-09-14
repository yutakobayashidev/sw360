package org.eclipse.sw360.clients.rest.resource.projects;

import java.util.List;

public class SW360ReleaseLinkJSON {
    private String releaseId;
    private List<SW360ReleaseLinkJSON> releaseLink;
    private String releaseRelationship;
    private String mainlineState;
    private String comment;
    private String createOn;
    private String createBy;

    public SW360ReleaseLinkJSON() {
    }

    public SW360ReleaseLinkJSON(String releaseId, List<SW360ReleaseLinkJSON> releaseLink, String releaseRelationship, String mainlineState, String comment, String createOn, String createBy) {
        this.releaseId = releaseId;
        this.releaseLink = releaseLink;
        this.releaseRelationship = releaseRelationship;
        this.mainlineState = mainlineState;
        this.comment = comment;
        this.createOn = createOn;
        this.createBy = createBy;
    }

    public String getReleaseId() {
        return releaseId;
    }

    public void setReleaseId(String releaseId) {
        this.releaseId = releaseId;
    }

    public List<SW360ReleaseLinkJSON> getReleaseLink() {
        return releaseLink;
    }

    public void setReleaseLink(List<SW360ReleaseLinkJSON> releaseLink) {
        this.releaseLink = releaseLink;
    }

    public String getReleaseRelationship() {
        return releaseRelationship;
    }

    public void setReleaseRelationship(String releaseRelationship) {
        this.releaseRelationship = releaseRelationship;
    }

    public String getMainlineState() {
        return mainlineState;
    }

    public void setMainlineState(String mainlineState) {
        this.mainlineState = mainlineState;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreateOn() {
        return createOn;
    }

    public void setCreateOn(String createOn) {
        this.createOn = createOn;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }
}
