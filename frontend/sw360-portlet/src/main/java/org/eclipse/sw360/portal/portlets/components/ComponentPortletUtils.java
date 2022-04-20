/*
 * Copyright Siemens AG, 2013-2018. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.components;

import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.*;
import org.eclipse.sw360.datahandler.thrift.components.*;
import org.eclipse.sw360.datahandler.thrift.licenses.LicenseType;
import org.eclipse.sw360.datahandler.thrift.licenses.Obligation;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.vendors.Vendor;
import org.eclipse.sw360.datahandler.thrift.vendors.VendorService;
import org.eclipse.sw360.datahandler.thrift.vulnerabilities.*;
import org.eclipse.sw360.portal.common.PortalConstants;
import org.eclipse.sw360.portal.common.PortletUtils;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import javax.portlet.PortletRequest;
import javax.portlet.ResourceRequest;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.datahandler.common.SW360Utils.newDefaultEccInformation;

/**
 * Component portlet implementation
 *
 * @author cedric.bodet@tngtech.com
 * @author Johannes.Najjar@tngtech.com
 * @author thomas.maier@evosoft.com
 */
public abstract class ComponentPortletUtils {

    private ComponentPortletUtils() {
        // Utility class with only static functions
    }

    public static void updateReleaseFromRequest(PortletRequest request, Release release) {
        for (Release._Fields field : Release._Fields.values()) {
            switch (field) {
                case REPOSITORY:
                    release.setFieldValue(field, getRepositoryFromRequest(request));
                    break;
                case CLEARING_INFORMATION:
                    release.setFieldValue(field, getClearingInformationFromRequest(request));
                    break;
                case ECC_INFORMATION:
                    release.setFieldValue(field, getEccInformationFromRequest(request));
                    break;
                case COTS_DETAILS:
                    release.setFieldValue(field, getCOTSDetailsFromRequest(request));
                    break;
                case VENDOR_ID:
                    release.unsetVendor();
                    setFieldValue(request, release, field);
                    break;
                case ATTACHMENTS:
                    release.setAttachments(PortletUtils.updateAttachmentsFromRequest(request, release.getAttachments()));
                    break;
                case RELEASE_ID_TO_RELATIONSHIP:
                    if (!release.isSetReleaseIdToRelationship())
                        release.setReleaseIdToRelationship(new HashMap<>());
                    updateLinkedReleaseFromRequest(request, release.releaseIdToRelationship);
                    break;
                case CLEARING_STATE:
                    // skip setting CLEARING_STATE. it is supposed to be set only programmatically, never from user input.
                    break;
                case ROLES:
                    release.setRoles(PortletUtils.getCustomMapFromRequest(request));
                    break;
                case EXTERNAL_IDS:
                    release.setExternalIds(PortletUtils.getExternalIdMapFromRequest(request));
                    break;
                case ADDITIONAL_DATA:
                    release.setAdditionalData(PortletUtils.getAdditionalDataMapFromRequest(request));
                    break;
                default:
                    setFieldValue(request, release, field);
            }
        }
    }

    private static ClearingInformation getClearingInformationFromRequest(PortletRequest request) {
        ClearingInformation clearingInformation = new ClearingInformation();
        for (ClearingInformation._Fields field : ClearingInformation._Fields.values()) {
            setFieldValue(request, clearingInformation, field);
        }

        return clearingInformation;
    }

    private static EccInformation getEccInformationFromRequest(PortletRequest request) {
        EccInformation eccInformation = newDefaultEccInformation();
        for (EccInformation._Fields field : EccInformation._Fields.values()) {
            setFieldValue(request, eccInformation, field);
        }

        return eccInformation;
    }

    private static COTSDetails getCOTSDetailsFromRequest(PortletRequest request) {
        COTSDetails cotsDetails = new COTSDetails();
        for (COTSDetails._Fields field : COTSDetails._Fields.values()) {
            setFieldValue(request, cotsDetails, field);
        }

        return cotsDetails;
    }

    private static Repository getRepositoryFromRequest(PortletRequest request) {
        Repository repository = new Repository();
        setFieldValue(request, repository, Repository._Fields.REPOSITORYTYPE);
        setFieldValue(request, repository, Repository._Fields.URL);

        if (!repository.isSetUrl() || isNullOrEmpty(repository.getUrl())) {
            repository = null;
        }
        return repository;
    }

    static void updateComponentFromRequest(PortletRequest request, Component component) {
        for (Component._Fields field : Component._Fields.values()) {
            switch (field) {
                case ATTACHMENTS:
                    component.setAttachments(PortletUtils.updateAttachmentsFromRequest(request, component.getAttachments()));
                    break;
                case ROLES:
                    component.setRoles(PortletUtils.getCustomMapFromRequest(request));
                case EXTERNAL_IDS:
                    component.setExternalIds(PortletUtils.getExternalIdMapFromRequest(request));
                    break;
                case ADDITIONAL_DATA:
                    component.setAdditionalData(PortletUtils.getAdditionalDataMapFromRequest(request));
                    break;
                default:
                    setFieldValue(request, component, field);
                    break;
            }
        }
    }

    private static List<Component._Fields> extractFieldsForComponentUpdate(List<String> requestParams, Component component) {
        return component.getId() == null
                ? Arrays.asList(Component._Fields.values())
                : Arrays.stream(Component._Fields.values())
                .filter(f -> requestParams.contains(f.name()))
                .collect(Collectors.toList());
    }

    public static void updateVendorFromRequest(PortletRequest request, Vendor vendor) {
        setFieldValue(request, vendor, Vendor._Fields.FULLNAME);
        setFieldValue(request, vendor, Vendor._Fields.SHORTNAME);
        setFieldValue(request, vendor, Vendor._Fields.URL);
    }

    public static void updateTodoFromRequest(PortletRequest request, Obligation oblig) {
        setFieldValue(request, oblig, Obligation._Fields.TITLE);
        setFieldValue(request, oblig, Obligation._Fields.TEXT);
        setFieldValue(request, oblig, Obligation._Fields.OBLIGATION_LEVEL);
        setFieldValue(request, oblig, Obligation._Fields.OBLIGATION_TYPE);
    }

    public static void updateLicenseTypeFromRequest(PortletRequest request, LicenseType licenseType) {
        setFieldValue(request, licenseType, LicenseType._Fields.LICENSE_TYPE);
    }

    private static void updateLinkedReleaseFromRequest(PortletRequest request, Map<String, ReleaseRelationship> linkedReleases) {
        linkedReleases.clear();
        String[] ids = request.getParameterValues(Release._Fields.RELEASE_ID_TO_RELATIONSHIP.toString() + ReleaseLink._Fields.ID.toString());
        String[] relations = request.getParameterValues(Release._Fields.RELEASE_ID_TO_RELATIONSHIP.toString() + ReleaseLink._Fields.RELEASE_RELATIONSHIP.toString());
        if (ids != null && relations != null && ids.length == relations.length)
            for (int k = 0; k < ids.length; ++k) {
                linkedReleases.put(ids[k], ReleaseRelationship.findByValue(Integer.parseInt(relations[k])));
            }
    }

    private static void setFieldValue(PortletRequest request, Component component, Component._Fields field) {
        PortletUtils.setFieldValue(request, component, field, Component.metaDataMap.get(field), "");
    }

    private static void setFieldValue(PortletRequest request, Vulnerability vulnerability, Vulnerability._Fields field) {
        PortletUtils.setFieldValue(request, vulnerability, field, Vulnerability.metaDataMap.get(field), "");
    }

    private static void setFieldValue(PortletRequest request, Release release, Release._Fields field) {
        PortletUtils.setFieldValue(request, release, field, Release.metaDataMap.get(field), "");
    }

    private static void setFieldValue(PortletRequest request, Repository repository, Repository._Fields field) {
        PortletUtils.setFieldValue(request, repository, field, Repository.metaDataMap.get(field), Release._Fields.REPOSITORY.toString());
    }

    private static void setFieldValue(PortletRequest request, ClearingInformation clearingInformation, ClearingInformation._Fields field) {
        PortletUtils.setFieldValue(request, clearingInformation, field, ClearingInformation.metaDataMap.get(field), Release._Fields.CLEARING_INFORMATION.toString());
    }

    private static void setFieldValue(PortletRequest request, EccInformation eccInformation, EccInformation._Fields field) {
        PortletUtils.setFieldValue(request, eccInformation, field, EccInformation.metaDataMap.get(field), Release._Fields.ECC_INFORMATION.toString());
    }

    private static void setFieldValue(PortletRequest request, COTSDetails cotsDetails, COTSDetails._Fields field) {
        PortletUtils.setFieldValue(request, cotsDetails, field, COTSDetails.metaDataMap.get(field), Release._Fields.COTS_DETAILS.toString());
    }

    private static void setFieldValue(PortletRequest request, Vendor vendor, Vendor._Fields field) {
        PortletUtils.setFieldValue(request, vendor, field, Vendor.metaDataMap.get(field), "");
    }

    private static void setFieldValue(PortletRequest request, Obligation oblig, Obligation._Fields field) {
        PortletUtils.setFieldValue(request, oblig, field, Obligation.metaDataMap.get(field), "");
    }

    private static void setFieldValue(PortletRequest request, LicenseType licenseType, LicenseType._Fields field) {
        PortletUtils.setFieldValue(request, licenseType, field, LicenseType.metaDataMap.get(field), "");
    }

    public static RequestStatus deleteRelease(PortletRequest request, Logger log) {
        String releaseId = request.getParameter(PortalConstants.RELEASE_ID);
        if (releaseId != null) {
            try {
                String deleteCommentEncoded = request.getParameter(PortalConstants.MODERATION_REQUEST_COMMENT);
                User user = UserCacheHolder.getUserFromRequest(request);
                if (deleteCommentEncoded != null) {
                    String deleteComment = new String(Base64.getDecoder().decode(deleteCommentEncoded));
                    user.setCommentMadeDuringModerationRequest(deleteComment);
                }
                ComponentService.Iface client = new ThriftClients().makeComponentClient();
                return client.deleteRelease(releaseId, UserCacheHolder.getUserFromRequest(request));

            } catch (TException e) {
                log.error("Could not delete release from DB", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static RequestStatus deleteVendor(PortletRequest request, Logger log) {
        String vendorId = request.getParameter(PortalConstants.VENDOR_ID);
        if (vendorId != null) {
            try {
                User user = UserCacheHolder.getUserFromRequest(request);
                ThriftClients thriftClients = new ThriftClients();
                ComponentService.Iface componentClient = thriftClients.makeComponentClient();
                VendorService.Iface client = thriftClients.makeVendorClient();

                RequestStatus global_status = RequestStatus.SUCCESS;

                List<Release> releases = componentClient.getReleasesFromVendorId(vendorId, user);

                boolean mayWriteToAllReleases = true;
                for (Release release : releases) {
                    Map<RequestedAction, Boolean> permissions = release.getPermissions();
                    mayWriteToAllReleases &= permissions.get(RequestedAction.WRITE);
                }

                if (!mayWriteToAllReleases) {
                    return RequestStatus.FAILURE;
                }

                for (Release release : releases) {
                    if (release.isSetVendorId()) {
                        release.unsetVendorId();
                    }
                    if (release.isSetVendor()) {
                        release.unsetVendor();
                    }
                    RequestStatus local_status = componentClient.updateRelease(release, user);
                    if (local_status != RequestStatus.SUCCESS) {
                        global_status = local_status;
                    }
                }

                if (global_status == RequestStatus.SUCCESS) {
                    return client.deleteVendor(vendorId, user);
                } else {
                    return global_status;
                }

            } catch (TException e) {
                log.error("Could not delete vendor from DB", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static RequestStatus deleteComponent(PortletRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.COMPONENT_ID);
        if (id != null) {
            try {
                String deleteCommentEncoded = request.getParameter(PortalConstants.MODERATION_REQUEST_COMMENT);
                User user = UserCacheHolder.getUserFromRequest(request);
                if (deleteCommentEncoded != null) {
                    String deleteComment = new String(Base64.getDecoder().decode(deleteCommentEncoded));
                    user.setCommentMadeDuringModerationRequest(deleteComment);
                }

                ComponentService.Iface client = new ThriftClients().makeComponentClient();
                return client.deleteComponent(id, user);

            } catch (TException e) {
                log.error("Could not delete component from DB", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static RequestStatus subscribeComponent(ResourceRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.COMPONENT_ID);
        if (id != null) {
            try {
                ComponentService.Iface client = new ThriftClients().makeComponentClient();
                User user = UserCacheHolder.getUserFromRequest(request);
                return client.subscribeComponent(id, user);

            } catch (TException e) {
                log.error("Could not subscribe to component", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static RequestStatus subscribeRelease(ResourceRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.RELEASE_ID);
        if (id != null) {
            try {
                ComponentService.Iface client = new ThriftClients().makeComponentClient();
                User user = UserCacheHolder.getUserFromRequest(request);
                return client.subscribeRelease(id, user);

            } catch (TException e) {
                log.error("Could not subscribe to release", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static RequestStatus unsubscribeComponent(ResourceRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.COMPONENT_ID);
        if (id != null) {
            try {
                ComponentService.Iface client = new ThriftClients().makeComponentClient();
                User user = UserCacheHolder.getUserFromRequest(request);
                return client.unsubscribeComponent(id, user);

            } catch (TException e) {
                log.error("Could not unsubscribe to component", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static RequestStatus unsubscribeRelease(ResourceRequest request, Logger log) {
        String id = request.getParameter(PortalConstants.RELEASE_ID);
        if (id != null) {
            try {
                ComponentService.Iface client = new ThriftClients().makeComponentClient();
                User user = UserCacheHolder.getUserFromRequest(request);
                return client.unsubscribeRelease(id, user);

            } catch (TException e) {
                log.error("Could not unsubscribe to release", e);
            }
        }
        return RequestStatus.FAILURE;
    }

    public static ReleaseVulnerabilityRelation updateReleaseVulnerabilityRelationFromRequest(ReleaseVulnerabilityRelation dbRelation, ResourceRequest request) {

        if (!dbRelation.isSetVerificationStateInfo()) {
            dbRelation.setVerificationStateInfo(new ArrayList<>());
        }
        List<VerificationStateInfo> verificationStateHistory = dbRelation.getVerificationStateInfo();

        VerificationState verificationState = VerificationState.findByValue(
                Integer.parseInt(request.getParameter(PortalConstants.VULNERABILITY_VERIFICATION_VALUE)));

        VerificationStateInfo resultInfo = new VerificationStateInfo()
                .setCheckedBy(UserCacheHolder.getUserFromRequest(request).getEmail())
                .setCheckedOn(SW360Utils.getCreatedOn())
                .setComment(request.getParameter(PortalConstants.VULNERABILITY_VERIFICATION_COMMENT))
                .setVerificationState(verificationState);
        verificationStateHistory.add(resultInfo);

        return dbRelation;
    }

    /**
     * Set data add vulnerability
     *
     * @param request
     * @param vulnerability
     */
    public static void updateVulnerabilityFromRequest(PortletRequest request, Vulnerability vulnerability) {

        // Format date time
        String formatDate = "yyyy-MM-dd";
        String formatTime = "HH:mm:ss";

        setFieldValue(request, vulnerability, Vulnerability._Fields.EXTERNAL_ID);
        setFieldValue(request, vulnerability, Vulnerability._Fields.TITLE);
        setFieldValue(request, vulnerability, Vulnerability._Fields.DESCRIPTION);
        setFieldValue(request, vulnerability, Vulnerability._Fields.PRIORITY);
        setFieldValue(request, vulnerability, Vulnerability._Fields.PRIORITY_TEXT);
        setFieldValue(request, vulnerability, Vulnerability._Fields.ACTION);
        setFieldValue(request, vulnerability, Vulnerability._Fields.LEGAL_NOTICE);

        // CVE
        String cwe = "CWE-" + request.getParameter(String.valueOf(Vulnerability._Fields.CWE));
        vulnerability.setCwe(cwe);
        setFieldValue(request, vulnerability, Vulnerability._Fields.EXTENDED_DESCRIPTION);

        // TODO
        // CVE futher meta data per source
        // setFieldValue(request, vulnerability, Vulnerability._Fields.DESCRIPTION);

        // Cvss Score
        // Get other information from request
        String cvss = request.getParameter(String.valueOf(Vulnerability._Fields.CVSS));
        vulnerability.setCvss(Double.parseDouble(cvss));
        // setFieldValue(request, vulnerability, Vulnerability._Fields.CVSS);

        String cvssDate = request.getParameter(PortalConstants.CVSS_DATE);
        String cvssTime = request.getParameter(PortalConstants.CVSS_TIME);
        String cvssDateTime = formatDate(cvssDate, cvssTime, formatDate, formatTime);
        vulnerability.setCvssTime(cvssDateTime);

        // Publish Date
        String publishDate = request.getParameter(PortalConstants.PUBLISH_DATE);
        String publishTime = request.getParameter(PortalConstants.PUBLISH_TIME);
        String publishDateTime = formatDate(publishDate, publishTime, formatDate, formatTime);
        vulnerability.setPublishDate(publishDateTime);

        // Get external update date time from request
        String externalUpdateDate = request.getParameter(PortalConstants.EXTERNAL_UPDATE_DATE);
        String externalUpdateTime = request.getParameter(PortalConstants.EXTERNAL_UPDATE_TIME);
        String externalUpdateDateTime = formatDate(externalUpdateDate, externalUpdateTime, formatDate, formatTime);
        vulnerability.setLastExternalUpdate(externalUpdateDateTime);

        // Vulnerability Impact
        String[] impactKeys = request.getParameterValues(PortalConstants.VULNERABILITY_IMPACT_KEY);
        String[] impactValues = request.getParameterValues(PortalConstants.VULNERABILITY_IMPACT_VALUE);
        Map<String, String> impacts = convertKeyValFromEnumToMap(impactKeys, impactValues);
        vulnerability.setImpact(impacts);

        // Vulnerability Access
        String[] accessKeys = request.getParameterValues(PortalConstants.VULNERABILITY_ACCESS_KEY);
        String[] accessValues = request.getParameterValues(PortalConstants.VULNERABILITY_ACCESS_VALUE);
        Map<String, String> accesses = convertKeyValFromEnumToMap(accessKeys, accessValues);
        vulnerability.setAccess(accesses);

        // Cve Reference
        Set<CVEReference> cveReferences = new HashSet<>();
        String[] cveYears = request.getParameterValues(PortalConstants.VULNERABILITY_CVE_YEAR);
        String[] cveNumbers = request.getParameterValues(PortalConstants.VULNERABILITY_CVE_NUMBER);
        for (int i = 0; i < cveYears.length; i++) {
            if (!"".equals(cveNumbers[i]) || !"".equals(cveNumbers[i])) {
                CVEReference cveReference = new CVEReference();
                cveReference.setYear(cveYears[i]);
                cveReference.setNumber(cveNumbers[i]);
                cveReferences.add(cveReference);
            }
        }
        vulnerability.setCveReferences(cveReferences);

        // Assigned External Component Ids
        Set<String> externalComponentIds = new HashSet<String>();
        String[] externalComponentIdArray = request.getParameterValues(String.valueOf(Vulnerability._Fields.ASSIGNED_EXT_COMPONENT_IDS));
        for (String componentId : externalComponentIdArray) {
            if (!"".equals(componentId)) {
                externalComponentIds.add(componentId);
            }
        }
        vulnerability.setAssignedExtComponentIds(externalComponentIds);

        // Vulnerability Reference
        Set<String> references = new HashSet<String>();
        String[] vulnerabilityReference = request.getParameterValues(String.valueOf(Vulnerability._Fields.REFERENCES));
        for (String reference : vulnerabilityReference) {
            if (!"".equals(reference)) {
                references.add(reference);
            }
        }
        vulnerability.setReferences(references);

        // Vendor Advisories
        Set<VendorAdvisory> vendorAdvisories = new HashSet<>();
        String[] advisoryVendors = request.getParameterValues(PortalConstants.VULNERABILITY_ADVISORY_VENDOR);
        String[] advisoryNames = request.getParameterValues(PortalConstants.VULNERABILITY_ADVISORY_NAME);
        String[] advisoryUrls = request.getParameterValues(PortalConstants.VULNERABILITY_ADVISORY_URL);
        for (int i = 0; i < advisoryVendors.length; i++) {
            if (!"".equals(advisoryVendors[i]) || !"".equals(advisoryNames[i]) || !"".equals(advisoryUrls[i])) {
                VendorAdvisory vendorAdvisory = new VendorAdvisory();
                vendorAdvisory.setVendor(advisoryVendors[i]);
                vendorAdvisory.setName(advisoryNames[i]);
                vendorAdvisory.setUrl(advisoryUrls[i]);
                vendorAdvisories.add(vendorAdvisory);
            }
        }
        vulnerability.setVendorAdvisories(vendorAdvisories);

        // Vulnerability Configuration
        String[] configKeys = request.getParameterValues(PortalConstants.VULNERABILITY_CONFIG_KEY);
        String[] configValues = request.getParameterValues(PortalConstants.VULNERABILITY_CONFIG_VALUE);
        Map<String, String> configs = convertKeyValFromEnumToMap(configKeys, configValues);
        vulnerability.setVulnerableConfiguration(configs);


    }

    /**
     * Delete vulnerability
     *
     * @param request
     * @param log
     * @return
     */
    public static RequestStatus deleteVulnerability(PortletRequest request, Logger log) {
        String vulnerabilityId = request.getParameter(PortalConstants.VULNERABILITY_ID);
        if (vulnerabilityId != null) {
            try {
                User user = UserCacheHolder.getUserFromRequest(request);
                ThriftClients thriftClients = new ThriftClients();

                VulnerabilityService.Iface vulnerabilityClient = thriftClients.makeVulnerabilityClient();
                Vulnerability vulnerability = vulnerabilityClient.getVulnerabilityId(vulnerabilityId);
                VulnerabilityWithReleaseRelations vulnerabilityWithReleaseRelations = vulnerabilityClient
                        .getVulnerabilityWithReleaseRelationsByExternalId(vulnerability.getExternalId(), user);
                if (!CommonUtils.isNotEmpty(vulnerabilityWithReleaseRelations.getReleaseRelation())) {
                    return vulnerabilityClient.deleteVulnerability(vulnerability, user);
                } else {
                    return RequestStatus.IN_USE;
                }
            } catch (Exception e) {
                log.error("DeleteVulnerability has error: ", e);
                return RequestStatus.FAILURE;
            }
        }
        return RequestStatus.FAILURE;
    }

    /**
     * Format date string input
     *
     * @param strDate
     * @param strTime
     * @param strFormatDate
     * @param strFormatTime
     * @return
     */
    public static String formatDate(String strDate, String strTime, String strFormatDate, String strFormatTime) {
        // Format date time
        SimpleDateFormat formatDate = new SimpleDateFormat(strFormatDate);
        SimpleDateFormat formatTime = new SimpleDateFormat(strFormatTime);
        String date = strDate;
        String time = strTime;

        if ("".equals(strDate)) {
            date = formatDate.format(new Date());
        }

        if ("".equals(strTime)) {
            time = formatTime.format(new Date());
        }

        return date + "T" + time + ".00000";
    }

    /**
     * Convert data from enum to map
     *
     * @param keys
     * @param values
     * @return Map<String, String>
     */
    public static Map<String, String> convertKeyValFromEnumToMap(String[] keys, String[] values) {
        Map<String, String> result = new HashMap<>();

        if (keys == null || keys.length == 0) {
            return result;
        }
        for (int i = 0; i < keys.length; i++) {
            if (!values[i].equals("") && !values[i].equals("")) {
                result.put(keys[i], values[i]);
            }
        }
        return result;
    }
}
