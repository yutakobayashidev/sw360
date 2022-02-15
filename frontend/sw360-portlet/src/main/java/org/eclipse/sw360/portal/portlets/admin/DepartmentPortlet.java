package org.eclipse.sw360.portal.portlets.admin;


import java.io.UnsupportedEncodingException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.components.ComponentPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;
import java.io.IOException;
import java.util.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@Component(
        immediate = true,
        properties = {
                "/org/eclipse/sw360/portal/portlets/base.properties",
                "/org/eclipse/sw360/portal/portlets/admin.properties"
        },
        property = {
                "javax.portlet.name=" + DEPARTMENT_PORTLET_NAME,
                "javax.portlet.display-name=Department",
                "javax.portlet.info.short-title=Department",
                "javax.portlet.info.title=Department",
                "javax.portlet.resource-bundle=content.Language",
                "javax.portlet.init-param.view-template=/html/admin/department/view.jsp",
        },
        service = Portlet.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class DepartmentPortlet extends Sw360Portlet {
    private static final Logger log = LogManager.getLogger(DepartmentPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);
        if (PAGENAME_EDIT.equals(pageName)) {
            prepareDepartmentEdit(request);
            include("/html/admin/department/edit.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }

    }

    private void prepareStandardView(RenderRequest request) {
        try {
            UserService.Iface userClient = thriftClients.makeUserClient();
            Map<String, List<User>> listMap = userClient.getAllUserByDepartment();
            request.setAttribute(PortalConstants.DEPARTMENT_LIST, listMap);
            Map<String, List<String>> allMessageError = userClient.getAllMessageError();
            LinkedHashMap<String, List<String>> sortedMap = new LinkedHashMap<>();
            allMessageError.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                    .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
            request.setAttribute("allMessageError", sortedMap);
            request.setAttribute("lastFileName", userClient.getLastModifiedFileName());
            User user = UserCacheHolder.getUserFromRequest(request);
            ScheduleService.Iface scheduleClient = new ThriftClients().makeScheduleClient();
            boolean isDepartmentScheduled = isDepartmentScheduled(scheduleClient, user);
            request.setAttribute(PortalConstants.DEPARTMENT_IS_SCHEDULED, isDepartmentScheduled);
            int offsetInSeconds = scheduleClient.getFirstRunOffset(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
            request.setAttribute(PortalConstants.DEPARTMENT_OFFSET, CommonUtils.formatTime(offsetInSeconds));
            int intervalInSeconds = scheduleClient.getInterval(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
            request.setAttribute(PortalConstants.DEPARTMENT_INTERVAL, CommonUtils.formatTime(intervalInSeconds));
            String nextSync = scheduleClient.getNextSync(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
            request.setAttribute(PortalConstants.DEPARTMENT_NEXT_SYNC, nextSync);
        } catch (TException te) {
            log.error("Error: {}", te.getMessage());
        }
    }

    private boolean isDepartmentScheduled(ScheduleService.Iface scheduleClient, User user) throws TException {
        RequestStatusWithBoolean requestStatus = scheduleClient.isServiceScheduled(ThriftClients.IMPORT_DEPARTMENT_SERVICE, user);
        if (RequestStatus.SUCCESS.equals(requestStatus.getRequestStatus())) {
            return requestStatus.isAnswerPositive();
        } else {
            throw new SW360Exception("Backend query for schedule status of department failed.");
        }
    }

    @UsedAsLiferayAction
    public void scheduleImportDepartment(ActionRequest request, ActionResponse response) throws PortletException {
        try {
            RequestSummary requestSummary =
                    new ThriftClients().makeScheduleClient().scheduleService(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
            setSessionMessage(request, requestSummary.getRequestStatus(), "Task", "schedule");
        } catch (TException e) {
            log.error("Schedule import department: {}", e.getMessage());
        }
    }

    @UsedAsLiferayAction
    public void unScheduleImportDepartment(ActionRequest request, ActionResponse response) throws PortletException {
        try {
            User user = UserCacheHolder.getUserFromRequest(request);
            RequestStatus requestStatus =
                    new ThriftClients().makeScheduleClient().unscheduleService(ThriftClients.IMPORT_DEPARTMENT_SERVICE, user);
            setSessionMessage(request, requestStatus, "Task", "unschedule");
        } catch (TException e) {
            log.error("Cancel Schedule import department: {}", e.getMessage());
        }
    }

    @UsedAsLiferayAction
    public void importDepartmentManually(ActionRequest request, ActionResponse response) throws PortletException {
        try {
            UserService.Iface userClient = thriftClients.makeUserClient();
            RequestStatus requestStatus = userClient.importDepartmentSchedule();
            setSessionMessage(request, requestStatus, "User", "Success");
        } catch (TException e) {
            log.error("Cancel Schedule import department: {}", e.getMessage());
        }
    }

    private void prepareDepartmentEdit(RenderRequest request) throws PortletException, UnsupportedEncodingException {
        String key = request.getParameter(DEPARTMENT_KEY);

        if (!isNullOrEmpty(key)) {
            try {
                UserService.Iface userClient = thriftClients.makeUserClient();
                String jsonEmail = userClient.searchUsersByDepartmentToJson(key);
                String jsonEmailOtherDepartment = userClient.getAllEmailOtherDepartmentToJson(key);

                request.setAttribute(EMAIL_OTHER_DEPARTMENT_JSON, jsonEmailOtherDepartment);
                request.setAttribute(DEPARTMENT_EMAIL_ROLE_JSON, jsonEmail);
                request.setAttribute(PortalConstants.DEPARTMENT_ENCODE, decodeString(encodeString(key)));
                request.setAttribute(PortalConstants.DEPARTMENT_KEY, encodeString(key));


            } catch (TException e) {
                log.error("Problem retrieving department");
            }
        }
    }

    @UsedAsLiferayAction
    public void updateDepartment(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String key = request.getParameter(DEPARTMENT_KEY);
        String department = decodeString(key);
        List<String> emails = ComponentPortletUtils.updateUserFromRequest(request, log);
        if (key != null) {
            try {
                UserService.Iface userClient = thriftClients.makeUserClient();
                userClient.updateDepartmentToListUser(emails, department);
            } catch (TException e) {
                log.error("Error fetching User from backend!", e);
            }
        }
    }

    @UsedAsLiferayAction
    public void removeDepartment(ActionRequest request, ActionResponse response) throws IOException, PortletException {
        final RequestStatus requestStatus = ComponentPortletUtils.deleteDepartment(request, log);
        setSessionMessage(request, requestStatus, "Department", "delete");
        response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
    }

    private void removeDepartment(PortletRequest request, ResourceResponse response) throws IOException {
        final RequestStatus requestStatus = ComponentPortletUtils.deleteDepartment(request, log);
        serveRequestStatus(request, response, requestStatus, "Problem removing Department", log);

    }

    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(ACTION);
        if (REMOVE_DEPARTMENT_BY_EMAIL.equals(action)) {
            removeDepartment(request, response);
        }
    }

    public static String encodeString(String text)
            throws UnsupportedEncodingException {
        byte[] bytes = text.getBytes("UTF-8");
        String encodeString = Base64.getEncoder().encodeToString(bytes);
        return encodeString;
    }

    public static String decodeString(String encodeText)
            throws UnsupportedEncodingException {
        byte[] decodeBytes = Base64.getDecoder().decode(encodeText);
        String str = new String(decodeBytes, "UTF-8");
        return str;
    }

}
