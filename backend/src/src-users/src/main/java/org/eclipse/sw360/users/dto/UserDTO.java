package org.eclipse.sw360.users.dto;

import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class UserDTO {
    private String id;
    private String email;
    private String revision;
    private String type;
    private String externalid;
    private String fullname;
    private String givenname;
    private String lastname;
    private String department;
    private Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Map<String, Set<UserGroup>> getSecondaryDepartmentsAndRoles() {
        return secondaryDepartmentsAndRoles;
    }

    public void setSecondaryDepartmentsAndRoles(Map<String, Set<UserGroup>> secondaryDepartmentsAndRoles) {
        this.secondaryDepartmentsAndRoles = secondaryDepartmentsAndRoles;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getExternalid() {
        return externalid;
    }

    public void setExternalid(String externalid) {
        this.externalid = externalid;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getGivenname() {
        return givenname;
    }

    public void setGivenname(String givenname) {
        this.givenname = givenname;
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public static UserDTO convertToUserDTO(User user) {
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setRevision(user.getRevision());
        userDTO.setType(user.getType());
        userDTO.setEmail(user.getEmail());
        userDTO.setGivenname(user.getGivenname());
        userDTO.setLastname(user.getLastname());
        userDTO.setExternalid(user.getExternalid());
        userDTO.setDepartment(user.getDepartment());
        userDTO.setFullname(user.getFullname());
        Map<String, Set<UserGroup>> map = new HashMap<>();
        userDTO.setSecondaryDepartmentsAndRoles(map);
        return userDTO;
    }

    public User convertToUser() {
        User user = new User();
        user.setEmail(this.getEmail());
        user.setDepartment(this.getDepartment());
        user.setSecondaryDepartmentsAndRoles(this.getSecondaryDepartmentsAndRoles());
        return user;
    }

    public User convertToUserUpdate() {
        User user = new User();
        user.setId(this.getId());
        user.setRevision(this.getRevision());
        user.setType(this.getType());
        user.setEmail(this.getEmail());
        user.setGivenname(this.getGivenname());
        user.setLastname(this.getLastname());
        user.setExternalid(this.getExternalid());
        user.setDepartment(this.getDepartment());
        user.setFullname(this.getFullname());
        user.setSecondaryDepartmentsAndRoles(this.getSecondaryDepartmentsAndRoles());
        return user;
    }
}
