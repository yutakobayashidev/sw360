/*
 * Copyright (c) Bosch Software Innovations GmbH 2016.
 * Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.vulnerabilities;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.liferay.portal.kernel.portlet.LiferayPortletURL;
import com.liferay.portal.kernel.portlet.PortletURLFactoryUtil;
import com.liferay.portal.kernel.theme.ThemeDisplay;
import com.liferay.portal.kernel.util.WebKeys;
import org.eclipse.sw360.datahandler.thrift.RequestStatus;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.portal.common.CustomFieldHelper;
import org.eclipse.sw360.portal.common.ErrorMessages;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.UsedAsLiferayAction;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.portlets.components.ComponentPortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.portlet.*;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Utils.printName;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
        immediate = true,
        properties = {
                "/org/eclipse/sw360/portal/portlets/base.properties",
                "/org/eclipse/sw360/portal/portlets/default.properties"
        },
        property = {
                "javax.portlet.name=" + VULNERABILITIES_PORTLET_NAME,

                "javax.portlet.display-name=Vulnerabilities",
                "javax.portlet.info.short-title=Vulnerabilities",
                "javax.portlet.info.title=Vulnerabilities",
                "javax.portlet.resource-bundle=content.Language",
                "javax.portlet.init-param.view-template=/html/vulnerabilities/view.jsp",
        },
        service = Portlet.class,
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class VulnerabilitiesPortlet extends Sw360Portlet {

    private static final Logger log = LogManager.getLogger(VulnerabilitiesPortlet.class);
    private static final String YEAR_MONTH_DAY_REGEX = "\\d\\d\\d\\d-\\d\\d-\\d\\d.*";

    private static final String EXTERNAL_ID = Vulnerability._Fields.EXTERNAL_ID.toString();
    private static final String VULNERABLE_CONFIGURATION = Vulnerability._Fields.VULNERABLE_CONFIGURATION.toString();

    public static final Set<Vulnerability._Fields> FILTERED_FIELDS = ImmutableSet.of(
            Vulnerability._Fields.EXTERNAL_ID,
            Vulnerability._Fields.VULNERABLE_CONFIGURATION
    );

    private static final int DEFAULT_VIEW_SIZE = 200;

    //Helper methods
    private void addVulnerabilityBreadcrumb(RenderRequest request, RenderResponse response, Vulnerability vulnerability) {
        PortletURL url = response.createRenderURL();
        url.setParameter(PAGENAME, PAGENAME_DETAIL);
        url.setParameter(VULNERABILITY_ID, vulnerability.getExternalId());

        addBreadcrumbEntry(request, printName(vulnerability), url);
    }

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        String pageName = request.getParameter(PAGENAME);

        if (PAGENAME_EDIT.equals(pageName)) {
            prepareVulnerabilityEdit(request);
            include("/html/vulnerabilities/edit.jsp", request, response);
        } else if (PAGENAME_DETAIL.equals(pageName)) {
            prepareDetailView(request, response);
            include("/html/vulnerabilities/detail.jsp", request, response);
        } else {
            prepareStandardView(request);
            super.doView(request, response);
        }
    }

    @UsedAsLiferayAction
    public void applyFilters(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        for (Vulnerability._Fields field : FILTERED_FIELDS) {
            response.setRenderParameter(field.toString(), nullToEmpty(request.getParameter(field.toString())));
        }
    }

    private void prepareStandardView(RenderRequest request) throws IOException {
        getFilteredVulnerabilityList(request);
    }

    private void getFilteredVulnerabilityList(PortletRequest request) throws IOException {
        List<Vulnerability> vulnerabilities = Collections.emptyList();
        int totalRows = 0;

        String externalId = request.getParameter(EXTERNAL_ID);
        String vulnerableConfig = request.getParameter(VULNERABLE_CONFIGURATION);

        try {
            final User user = UserCacheHolder.getUserFromRequest(request);
            int limit = CustomFieldHelper.loadAndStoreStickyViewSize(request, user, CUSTOM_FIELD_VULNERABILITIES_VIEW_SIZE);

            VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
            if (!isNullOrEmpty(externalId) || !isNullOrEmpty(vulnerableConfig)) {
                vulnerabilities = vulnerabilityClient.getVulnerabilitiesByExternalIdOrConfiguration(externalId, vulnerableConfig, user);
                totalRows = vulnerabilities.size();

                if (limit > 0) {
                    vulnerabilities = vulnerabilities.stream().limit(limit).collect(Collectors.toList());
                }
            } else {
                vulnerabilities = vulnerabilityClient.getLatestVulnerabilities(user, limit);
                totalRows = vulnerabilityClient.getTotalVulnerabilityCount(user);
            }
        } catch (TException e) {
            log.error("Could not search components in backend ", e);
        }

        shortenTimeStampsToDates(vulnerabilities);

        for (Vulnerability._Fields field : FILTERED_FIELDS) {
            request.setAttribute(field.getFieldName(), nullToEmpty(request.getParameter(field.toString())));
        }
        request.setAttribute(TOTAL_ROWS, totalRows);
        request.setAttribute(VULNERABILITY_LIST, vulnerabilities);
    }


    private void shortenTimeStampsToDates(List<Vulnerability> vulnerabilities) {
        vulnerabilities.stream().forEach(v -> {
            if (isFormattedTimeStamp(v.getPublishDate())) {
                v.setPublishDate(getDateFromFormattedTimeStamp(v.getPublishDate()));
            }
            if (isFormattedTimeStamp(v.getLastExternalUpdate())) {
                v.setLastExternalUpdate(getDateFromFormattedTimeStamp(v.getLastExternalUpdate()));
            }
            if (v.isSetCvssTime() && isFormattedTimeStamp(v.getCvssTime())) {
                v.setCvssTime(getDateFromFormattedTimeStamp(v.getCvssTime()));
            }
        });
    }

    private String getDateFromFormattedTimeStamp(String formattedTimeStamp) {
        return formattedTimeStamp.substring(0, 10);
    }

    private boolean isFormattedTimeStamp(String potentialTimestamp) {
        if (isNullOrEmpty(potentialTimestamp)) {
            return false;
        } else {
            return potentialTimestamp.matches(YEAR_MONTH_DAY_REGEX);
        }
    }

    private void prepareDetailView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        User user = UserCacheHolder.getUserFromRequest(request);
        String externalId = request.getParameter(VULNERABILITY_ID);
        if (externalId != null) {
            try {
                VulnerabilityService.Iface client = thriftClients.makeVulnerabilityClient();
                VulnerabilityWithReleaseRelations vulnerabilityWithReleaseRelations = client.getVulnerabilityWithReleaseRelationsByExternalId(externalId, user);

                if (vulnerabilityWithReleaseRelations != null) {
                    Vulnerability vulnerability = vulnerabilityWithReleaseRelations.getVulnerability();
                    List<Release> releases = getReleasesFromRelations(user, vulnerabilityWithReleaseRelations);

                    request.setAttribute(VULNERABILITY, vulnerability);
                    request.setAttribute(DOCUMENT_ID, externalId);
                    request.setAttribute(USING_RELEASES, releases);

                    addVulnerabilityBreadcrumb(request, response, vulnerability);
                }

            } catch (TException e) {
                log.error("Error fetching vulnerability from backend!", e);
            }
        }
    }

    private List<Release> getReleasesFromRelations(User user, VulnerabilityWithReleaseRelations vulnerabilityWithReleaseRelations) {
        if (vulnerabilityWithReleaseRelations != null) {
            List<ReleaseVulnerabilityRelation> relations = vulnerabilityWithReleaseRelations.getReleaseRelation();

            Set<String> ids = relations.stream()
                    .map(ReleaseVulnerabilityRelation::getReleaseId)
                    .collect(Collectors.toSet());

            try {
                ComponentService.Iface client = thriftClients.makeComponentClient();
                return client.getReleasesById(ids, user);
            } catch (TException e) {
                log.error("Error fetching releases from backend!", e);
            }
        }
        return ImmutableList.of();
    }

    private void prepareVulnerabilityEdit(RenderRequest request) {
        // Get user login
        User user = UserCacheHolder.getUserFromRequest(request);
        // Get para to request
        String id = request.getParameter(VULNERABILITY_ID);
        String dataGetAttribute = (String) request.getAttribute(VULNERABILITY_ID);

        // Set list value for tag select
        List<VulnerabilityImpact> vulnerabilityImpacts = new ArrayList<VulnerabilityImpact>(Arrays.asList(VulnerabilityImpact.values()));
        List<VulnerabilityAccessAuthentication> vulnerabilityAccessAuthentications = new ArrayList<VulnerabilityAccessAuthentication>(Arrays.asList(VulnerabilityAccessAuthentication.values()));
        List<VulnerabilityAccessComplexity> vulnerabilityAccessComplexities = new ArrayList<VulnerabilityAccessComplexity>(Arrays.asList(VulnerabilityAccessComplexity.values()));
        List<VulnerabilityAccessVector> vulnerabilityAccessVectors = new ArrayList<VulnerabilityAccessVector>(Arrays.asList(VulnerabilityAccessVector.values()));

        // Case is update vulnerability
        if (id != null) {
            try {
                // Get vulnerability by id
                VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
                Vulnerability vulnerability = vulnerabilityClient.getVulnerabilityId(id);
                request.setAttribute(VULNERABILITY, vulnerability);

            } catch (TException e) {
                log.error("Error fetching vulnerability from backend!", e);
            }
        }

        // Set data send view
        request.setAttribute("vulnerabilityImpacts", vulnerabilityImpacts);
        request.setAttribute("vulnerabilityAccessAuthentications", vulnerabilityAccessAuthentications);
        request.setAttribute("vulnerabilityAccessComplexities", vulnerabilityAccessComplexities);
        request.setAttribute("vulnerabilityAccessVectors", vulnerabilityAccessVectors);
    }

    private void addVulnerability(ActionRequest request, ActionResponse response) {
        Vulnerability vulnerability = new Vulnerability();
        final User user = UserCacheHolder.getUserFromRequest(request);

        String cvssDate = request.getParameter(PortalConstants.CVSS_DATE);
        String cvssTime = request.getParameter(PortalConstants.CVSS_TIME);
        String publishDate = request.getParameter(PortalConstants.PUBLISH_DATE);
        String publishTime = request.getParameter(PortalConstants.PUBLISH_TIME);
        String externalUpdateDate = request.getParameter(PortalConstants.EXTERNAL_UPDATE_DATE);
        String externalUpdateTime = request.getParameter(PortalConstants.EXTERNAL_UPDATE_TIME);
        log.info("cvssDate addd: " + cvssDate);
        log.info("cvssTime addd: " + cvssTime);
        log.info("publishDate addd: " + publishDate);
        log.info("publishTime addd: " + publishTime);
        log.info("externalUpdateDate addd: " + externalUpdateDate);
        log.info("externalUpdateTime addd: " + externalUpdateTime);

        try {
            VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
            ComponentPortletUtils.updateVulnerabilityFromRequest(request, vulnerability);
            vulnerability.setIsSetCvss(true);
            log.info("Object vulnerability addd: " + vulnerability);
            RequestStatus requestStatus = vulnerabilityClient.addVulnerability(vulnerability, user);
            setSessionMessage(request, requestStatus, "Vulnerability", "add", vulnerability.getExternalId());
            removeParamUrl(request, response);
        } catch (TException e) {
            log.error("Error adding vulnerability", e);
        } catch (PortletException e) {
            e.printStackTrace();
        }
    }
    @UsedAsLiferayAction
    public void updateVulnerability(ActionRequest request, ActionResponse response) throws PortletException, IOException {
        String id = request.getParameter(VULNERABILITY_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);
        if (id != null) {
            try {
                VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
                Vulnerability vulnerability = vulnerabilityClient.getVulnerabilityId(id);
                if (vulnerability == null) {
                    log.error("Error fetching vulnerability from backend!");
                    throw new TException();
                }
                ComponentPortletUtils.updateVulnerabilityFromRequest(request, vulnerability);
                vulnerability.setIsSetCvss(true);
                RequestStatus requestStatus = vulnerabilityClient.updateVulnerability(vulnerability, user);
                setSessionMessage(request, requestStatus, "Vulnerability", "update", vulnerability.getExternalId());
                removeParamUrl(request, response);
            } catch (TException e) {
                log.error("Error fetching vulnerability from backend!", e);
            }
        } else {
            addVulnerability(request, response);
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
            log.info("Error remove param url: {}", e.getMessage());
        }
    }

    @Override
    public void serveResource(ResourceRequest request, ResourceResponse response) throws IOException, PortletException {
        String action = request.getParameter(ACTION);

        // Remove vulnerability
        if (REMOVE_VULNERABILITY.equals(action)) {
            removeVulnerabilityToRequestAjax(request, response);
        }
        // Check exist external id
        if (FIND_BY_EXTERNAL_ID.equals(action)) {
            RequestStatus requestStatus = getVulnerabilityByExternalId(request, response);
            serveRequestStatus(request, response, requestStatus, "", log);
        }
    }

    /**
     * Check vulnerability external id
     * @param request
     * @param response
     * @return
     */
    public RequestStatus getVulnerabilityByExternalId(ResourceRequest request, ResourceResponse response) {
        String vulnerabilityExternalId = request.getParameter(VULNERABILITY_EXTERNAL_ID);
        final User user = UserCacheHolder.getUserFromRequest(request);
        VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
        try {
            Vulnerability vulnerability = vulnerabilityClient.getVulnerabilityByExternalId(vulnerabilityExternalId, user);
            if (vulnerability == null) {
                return RequestStatus.SUCCESS;
            } else {
                return RequestStatus.DUPLICATE;
            }
        } catch (TException e) {
            log.info("Function  getVulnerabilityByExternalId has error: " + e) ;
            return RequestStatus.SUCCESS;
        }
    }

    /**
     * Remove vulnerability call view edit
     * @param request
     * @param response
     * @throws IOException
     * @throws PortletException
     */
    @UsedAsLiferayAction
    public void removeVulnerability(ActionRequest request, ActionResponse response) throws IOException, PortletException {
        final RequestStatus requestStatus = ComponentPortletUtils.deleteVulnerability(request, log);
        if (requestStatus == RequestStatus.SUCCESS) {
            setSessionMessage(request, requestStatus, "Vulnerability", "remove");
        } else if (requestStatus == RequestStatus.IN_USE) {
            response.setRenderParameter(PAGENAME, PAGENAME_EDIT);
            response.setRenderParameter(VULNERABILITY_ID, request.getParameter(VULNERABILITY_ID));
            setSW360SessionError(request, ErrorMessages.ERROR_VULNERABILITY_USED_BY_RELEASE);
        } else if (requestStatus == RequestStatus.FAILURE) {
            setSessionMessage(request, requestStatus, "Vulnerability", "remove");
        }

    }

    /**
     * Remove vulnerablity view list
     * @param request
     * @param response
     */
    private void removeVulnerabilityToRequestAjax(ResourceRequest request, ResourceResponse response) {
        final RequestStatus requestStatus = ComponentPortletUtils.deleteVulnerability(request, log);

        if (requestStatus == RequestStatus.SUCCESS) {
            serveRequestStatus(request, response, requestStatus, "Vulnerability " + request.getParameter(VULNERABILITY_ID) + " has been deleted", log);
        } else if (requestStatus == RequestStatus.IN_USE) {
            serveRequestStatus(request, response, requestStatus, ErrorMessages.ERROR_VULNERABILITY_USED_BY_RELEASE, log);
        } else {
            serveRequestStatus(request, response, requestStatus, ErrorMessages.ERROR_VULNERABILITY_DELETE, log);
        }
    }
}
