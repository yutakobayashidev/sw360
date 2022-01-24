package org.eclipse.sw360.portal.portlets.admin;

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
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.eclipse.sw360.portal.common.PortalConstants.DEPARTMENT_PORTLET_NAME;

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
        prepareStandardView(request);
        super.doView(request, response);
    }

    private void prepareStandardView(RenderRequest request) {
        try {
            UserService.Iface userClient = thriftClients.makeUserClient();
            Map<String, List<User>> listMap = userClient.getAllUserByDepartment();
            request.setAttribute(PortalConstants.DEPARTMENT_LIST, listMap);
            List<String> stringList = userClient.getMessageError();
            request.setAttribute("stringList",stringList);
            Set<String> listFileLogs = userClient.getListFileLog();
            request.setAttribute("listFileLogs",listFileLogs);
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

    @UsedAsLiferayAction
    public void showMessageErrors(ActionRequest request, ActionResponse response) throws PortletException {
        try {
            UserService.Iface userClient = thriftClients.makeUserClient();
            List<String> stringList = userClient.getMessageError();
            request.setAttribute("stringList",stringList);
        } catch (TException e) {
            log.error("Error: {}", e.getMessage());
        }
    }

}
