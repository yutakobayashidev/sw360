package org.eclipse.sw360.portal.portlets.admin;

import com.google.common.collect.Sets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.components.ComponentPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.graalvm.compiler.lir.LIRInstruction;
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
    private void prepareDepartmentEdit(RenderRequest request) throws PortletException {
        String key = request.getParameter(DEPARTMENT_KEY);

        if (!isNullOrEmpty(key)) {
            try {
                UserService.Iface userClient = thriftClients.makeUserClient();
                Map<String, List<User>> listMap = userClient.searchUsersByDepartment(key);
                List<String> departments=userClient.getAllDepartment();
                // set key alway begin
                departments.add(0,key);
                for (int i = 1; i <departments.size() ; i++) {
                    if(departments.get(i).equals(key)){
                        departments.remove(i);
                    }
                }
                List<String> emailsByDepartment=userClient.getAllEmailByDepartment(key);
                List<String> emails=userClient.getAllEmailOtherDepartment(key);
                request.setAttribute(PortalConstants.DEPARTMENT_KEY, key);
                request.setAttribute(PortalConstants.DEPARTMENT_LIST, listMap);
                request.setAttribute(PortalConstants.DEPARTMENT_NAME, departments);
                request.setAttribute(PortalConstants.LIST_EMAIL_BY_DEPARTMENT,emailsByDepartment);
                request.setAttribute(PortalConstants.LIST_EMAIL_OTHER_DEPARTMENT,emails);
            } catch (TException e) {
                log.error("Problem retrieving department");
            }
        }
    }
    @UsedAsLiferayAction
    public void updateDepartment(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String key = request.getParameter(DEPARTMENT_KEY);
        List<String> emails=new ArrayList<>();
         emails=ComponentPortletUtils.updateUserFromRequest(request);
        if (key != null) {
            try {
                log.info("emaillll-------------------------------"+emails);
                UserService.Iface userClient = thriftClients.makeUserClient();
                userClient.updateDepartmentToListUser(emails,key);

            } catch (TException e) {
                log.error("Error fetching vendor from backend!", e);
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
}
