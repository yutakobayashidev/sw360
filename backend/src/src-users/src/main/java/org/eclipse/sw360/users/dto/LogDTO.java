package org.eclipse.sw360.users.dto;

public class LogDTO {
    private String fileName;
    private String text;

    public LogDTO() {
    }

    public LogDTO(String fileName, String text) {
        this.fileName = fileName;
        this.text = text;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
