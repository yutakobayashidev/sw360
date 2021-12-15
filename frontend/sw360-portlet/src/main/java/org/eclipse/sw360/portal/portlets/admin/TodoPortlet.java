/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.admin;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseService;
import org.eclipse.sw360.datahandler.thrift.changelogs.*;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationElement;
import org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.components.ComponentPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

import javax.portlet.*;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import static com.google.common.base.Strings.isNullOrEmpty;

import static org.eclipse.sw360.portal.common.PortalConstants.*;
import org.eclipse.sw360.portal.common.*;
import com.liferay.portal.kernel.json.*;
import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/admin.properties"
    },
    property = {
        "javax.portlet.name=" + TODOS_PORTLET_NAME,

        "javax.portlet.display-name=Obligations",
        "javax.portlet.info.short-title=Obligations",
        "javax.portlet.info.title=Obligations",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/admin/obligations/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class TodoPortlet extends Sw360Portlet {

    private static final Logger log = LogManager.getLogger(TodoPortlet.class);

    //! Serve resource and helpers
    @Override

    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(ACTION);
        String where = request.getParameter(WHERE);
        final String id = request.getParameter("id");
        final User user = UserCacheHolder.getUserFromRequest(request);

        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
        ChangeLogsService.Iface changeLogsClient = thriftClients.makeChangeLogsClient();

        if (REMOVE_TODO.equals(action)) {
            try {
                RequestStatus status = changeLogsClient.deleteChangeLogsByDocumentId(id, user);
                status = licenseClient.deleteObligations(id, user);
                renderRequestStatus(request,response, status);
            } catch (TException e) {
                log.error("Error deleting oblig", e);
                renderRequestStatus(request,response, RequestStatus.FAILURE);
            }
        } else if (VIEW_IMPORT_OBLIGATION_ELEMENTS.equals(action)) {
            serveObligationElementSearchResults(request, response, where);
        } else if (LOAD_CHANGE_LOGS.equals(action) || VIEW_CHANGE_LOGS.equals(action)) {
            ChangeLogsPortletUtils changeLogsPortletUtilsPortletUtils = PortletUtils.getChangeLogsPortletUtils(thriftClients);
            JSONObject dataForChangeLogs = changeLogsPortletUtilsPortletUtils.serveResourceForChangeLogs(request, response, action);
            writeJSON(request, response, dataForChangeLogs);
        }
    }

    private void serveObligationElementSearchResults(ResourceRequest request, ResourceResponse response, String searchText) throws IOException, PortletException {
        List<ObligationElement> searchResult;
        try {
            LicenseService.Iface client = thriftClients.makeLicenseClient();
            if (isNullOrEmpty(searchText)) {
                searchResult = client.getObligationElements();
            } else {
                searchResult = client.searchObligationElement(searchText);
            }
        } catch (TException e) {
            log.error("Error searching Obligation Element", e);
            searchResult = Collections.emptyList();
        }
        request.setAttribute(OBLIGATION_ELEMENT_SEARCH, searchResult);
        include("/html/admin/obligations/ajax/searchObligationElementsAjax.jsp", request, response, PortletRequest.RESOURCE_PHASE);
    }


    //! VIEW and helpers
    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {

        String pageName = request.getParameter(PAGENAME);
        String obligatonId = request.getParameter(DOCUMENT_ID);

        if (PAGENAME_ADD.equals(pageName) || PAGENAME_EDIT.equals(pageName) || PAGENAME_DUPLICATE.equals(pageName)) {
            List<ObligationNode> obligationNodeList;
            List<ObligationElement> obligationElementList;
            List<Obligation> obligationList;
            LicenseService.Iface licenseClient = null;
            String obligationJson = "{\"text\" : \"\", \"title\": \"\"}";
            String obligationNodeListJson = "{}";
            String obligationElementListJson = "{}";
            String obligationListJson = "{}";
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                licenseClient = thriftClients.makeLicenseClient();
                obligationNodeList = licenseClient.getObligationNodes();
                obligationElementList = licenseClient.getObligationElements();
                obligationList = licenseClient.getObligations();
            } catch (Exception e) {
                log.error("Could not get Obligation node from backend ", e);
                obligationNodeList = Collections.emptyList();
                obligationElementList = Collections.emptyList();
                obligationList = Collections.emptyList();
            }
            request.setAttribute(OBLIGATION_NODE_LIST, obligationNodeList);
            request.setAttribute(OBLIGATION_ELEMENT_LIST, obligationElementList);
            request.setAttribute(TODO_LIST, obligationList);
            request.setAttribute(OBLIGATION_EDIT, new Obligation());
            request.setAttribute(OBLIGATION_ACTION, "");

            if (PAGENAME_EDIT.equals(pageName) || PAGENAME_DUPLICATE.equals(pageName)) {

                try {
                    Obligation obligation = licenseClient.getObligationsById(obligatonId);
                    try {
                        obligation.setNode("");
                        obligationJson = objectMapper.writeValueAsString(obligation);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }

                    request.setAttribute(OBLIGATION_EDIT, obligation);
                    if (PAGENAME_EDIT.equals(pageName)) {
                        request.setAttribute(OBLIGATION_ACTION, "edit");
                    } else {
                        request.setAttribute(OBLIGATION_ACTION, "duplicate");
                    }
                } catch (Exception e) {
                    log.error("Could not get Obligation from backend ", e);
                }
            }

            try {
                obligationNodeListJson = objectMapper.writeValueAsString(obligationNodeList);
                obligationElementListJson = objectMapper.writeValueAsString(obligationElementList);
                obligationListJson = objectMapper.writeValueAsString(obligationList);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
            request.setAttribute("obligationJson", obligationJson);
            request.setAttribute("obligationNodeListJson", obligationNodeListJson);
            request.setAttribute("obligationElementListJson", obligationElementListJson);
            request.setAttribute("obligationListJson", obligationListJson);
            include("/html/admin/obligations/add.jsp", request, response);
        } else if ("obligationchangelog".equals(pageName)) {
            try {
                LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
                Obligation obligation = licenseClient.getObligationsById(obligatonId);
                request.setAttribute("obligationName", obligation.getTitle());
                include("/html/admin/obligations/includes/obligationChangelog.jsp", request, response);
            } catch (Exception e) {
                log.error("Could not get Obligation from backend ", e);
            }
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    private void prepareStandardView(RenderRequest request) {
        List<Obligation> obligList;
        try {
            final User user = UserCacheHolder.getUserFromRequest(request);
            LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();

            obligList = licenseClient.getObligations();

        } catch (TException e) {
            log.error("Could not get Obligation from backend ", e);
            obligList = Collections.emptyList();
        }

        request.setAttribute(TODO_LIST, obligList);
    }

    @UsedAsLiferayAction
    public void addObligations(ActionRequest request, ActionResponse response) {
        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
        final User user = UserCacheHolder.getUserFromRequest(request);

        String action = request.getParameter(PortalConstants.OBLIGATION_ACTION);
        String obligationEditedId = request.getParameter(PortalConstants.OBLIGATION_ID);
        if (isNullOrEmpty(obligationEditedId) || action.equals("duplicate")) {
            try {
                final Obligation oblig = new Obligation();
                setObligationValues(request, oblig);
                String obligId = licenseClient.addObligations(oblig, user);
                if (isNullOrEmpty(obligId)) {
                    setSW360SessionError(request, ErrorMessages.OBLIGATION_NOT_ADDED);
                } else {
                    setSessionMessage(request, RequestStatus.SUCCESS, "Obligation", "adde", oblig.getTitle());
                }
            } catch (Exception e) {
                setSW360SessionError(request, ErrorMessages.OBLIGATION_NOT_ADDED);
                log.error("Error adding oblig", e);
            }
        } else {
            try {
                final Obligation oblig = licenseClient.getObligationsById(obligationEditedId);
                setObligationValues(request, oblig);
                String obligId = licenseClient.updateObligation(oblig, user);
                if (isNullOrEmpty(obligId)) {
                    setSW360SessionError(request, ErrorMessages.OBLIGATION_NOT_UPDATED);
                } else {
                    setSessionMessage(request, RequestStatus.SUCCESS, "Obligation", "update", oblig.getTitle());
                }
            } catch (Exception e) {
                setSW360SessionError(request, ErrorMessages.OBLIGATION_NOT_UPDATED);
                log.error("Error editing oblig", e);
            }
        }

        try {
            String portletId = (String) request.getAttribute(WebKeys.PORTLET_ID);
            ThemeDisplay tD = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
            long plid = tD.getPlid();
            LiferayPortletURL redirectUrl = PortletURLFactoryUtil.create(request, portletId, plid, PortletRequest.RENDER_PART);
            request.setAttribute(WebKeys.REDIRECT, redirectUrl.toString());
            response.sendRedirect(redirectUrl.toString());
        } catch (IOException e) {
            log.error("Error when send redirect url", e);
        }
    }

    private Obligation setObligationValues(ActionRequest request, Obligation oblig) throws TException {
        LicenseService.Iface licenseClient = thriftClients.makeLicenseClient();
        final User user = UserCacheHolder.getUserFromRequest(request);

        ComponentPortletUtils.updateTodoFromRequest(request, oblig);
        String jsonString = request.getParameter(Obligation._Fields.TEXT.toString());
        String obligationType = request.getParameter(Obligation._Fields.OBLIGATION_TYPE.toString());
        String obligationNode = licenseClient.addNodes(jsonString, user);
        String obligationText = licenseClient.buildObligationText(obligationNode, "0");
        oblig.setText(obligationText);
        oblig.setNode(obligationNode);
        oblig.setTitle(oblig.getTitle().trim());
        if (isNullOrEmpty(obligationType)) {
            oblig.setObligationType(null);
        }
        return oblig;
    }
}
