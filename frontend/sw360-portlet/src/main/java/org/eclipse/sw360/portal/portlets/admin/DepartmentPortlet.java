package org.eclipse.sw360.portal.portlets.admin;

import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.schedule.ScheduleService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.portal.common.ErrorMessages;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import javax.portlet.*;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    DateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy");

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        prepareStandardView(request);
        super.doView(request, response);
    }

    private void prepareStandardView(RenderRequest request) {
        try {
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
            dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date nextSyncDate = dateFormat.parse(nextSync);
            long seconds = nextSyncDate.getTime() / 1000 - intervalInSeconds;
            String lastRunningTime = dateFormat.format(new Date(seconds * 1000));
            request.setAttribute(PortalConstants.LAST_RUNNING_TIME, lastRunningTime);

            UserService.Iface userClient = thriftClients.makeUserClient();
            Map<String, List<User>> listMap = userClient.getAllUserByDepartment();
            request.setAttribute(PortalConstants.DEPARTMENT_LIST, listMap);
            Map<String, List<String>> allMessageError = userClient.getAllMessageError();
            LinkedHashMap<String, List<String>> sortedMap = new LinkedHashMap<>();
            allMessageError.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.reverseOrder()))
                    .forEachOrdered(x -> sortedMap.put(x.getKey(), x.getValue()));
            request.setAttribute(PortalConstants.ALL_MESSAGE_ERROR, sortedMap);
            String pathConfigFolderDepartment = userClient.getPathConfigDepartment();
            request.setAttribute(PortalConstants.PATH_CONFIG_FOLDER_DEPARTMENT, pathConfigFolderDepartment);
            request.setAttribute(PortalConstants.LAST_FILE_NAME, userClient.getLastModifiedFileName());
        } catch (TException | ParseException e) {
            log.error("Error: {}", e.getMessage());
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
            removeParamUrl(request, response);
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
            removeParamUrl(request, response);
        } catch (TException e) {
            log.error("Cancel Schedule import department: {}", e.getMessage());
        }
    }

//    @UsedAsLiferayAction
//    public void importDepartmentManually(ActionRequest request, ActionResponse response) throws PortletException {
//        try {
//            UserService.Iface userClient = thriftClients.makeUserClient();
//            RequestSummary requestSummary = userClient.importFileToDB();
//            renderRequestSummary(request, response, requestSummary);
////            setSessionMessage(request, requestStatus, "Department", "Success");
//            removeParamUrl(request, response);
//        } catch (TException e) {
//            log.error("Cancel Schedule import department: {}", e.getMessage());
//        }
//    }

    @UsedAsLiferayAction
    public void writePathFolder(ActionRequest request, ActionResponse response) throws PortletException {
        String path = request.getParameter(PortalConstants.DEPARTMENT_URL);
        try {
            UserService.Iface userClient = thriftClients.makeUserClient();
            userClient.writePathFolderConfig(path);
            setSessionMessage(request, RequestStatus.SUCCESS, "Edit folder path", "Success");
            removeParamUrl(request, response);
        } catch (TException e) {
            log.error("Error edit folder path in backend!", e);
            setSW360SessionError(request, ErrorMessages.DEFAULT_ERROR_MESSAGE);
        }
    }

    public void removeParamUrl(ActionRequest request, ActionResponse response) {
        try {
            String portletId = (String) request.getAttribute(WebKeys.PORTLET_ID);
            ThemeDisplay tD = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            long plid = tD.getPlid();
            LiferayPortletURL redirectUrl = PortletURLFactoryUtil.create(request, portletId, plid, PortletRequest.RENDER_PART);
            request.setAttribute(WebKeys.REDIRECT, redirectUrl.toString());
            response.sendRedirect(redirectUrl.toString());
        } catch (IOException e) {
            log.info("Error: {}", e.getMessage());
        }
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws PortletException {
        String action = request.getParameter(PortalConstants.ACTION);
        if (action == null) {
            log.error("Invalid action 'null'");
            return;
        }
        switch (action) {
            case PortalConstants.IMPORT_DEPARTMENT_MANUALLY:
                try {
                    importDepartmentManually(request, response);
                } catch (TException e) {
                    log.error("Something went wrong with the department", e);
                }
                break;
            default:
                log.warn("The LicenseAdminPortlet was called with unsupported action=[" + action + "]");
        }
    }

    private void importDepartmentManually(ResourceRequest request, ResourceResponse response) throws TException {
        UserService.Iface userClient = thriftClients.makeUserClient();
        RequestSummary requestSummary = userClient.importFileToDB();
        log.info("****************importDepartmentManually***********"+requestSummary);
        renderRequestSummary(request, response, requestSummary);
    }

}
