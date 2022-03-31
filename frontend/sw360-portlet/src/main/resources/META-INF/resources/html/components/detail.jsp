<%--
  ~ Copyright Siemens AG, 2013-2019. Part of the SW360 Portal Project.
  ~ With modifications by Bosch Software Innovations GmbH, 2016.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<c:catch var="attributeNotFoundException">
    <jsp:useBean id="component" class="org.eclipse.sw360.datahandler.thrift.components.Component" scope="request"/>
    <jsp:useBean id="usingProjects" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.projects.Project>" scope="request"/>
    <jsp:useBean id="usingComponents" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.components.Component>" scope="request"/>
    <jsp:useBean id="allUsingProjectsCount" type="java.lang.Integer" scope="request"/>
    <jsp:useBean id="documentType" class="java.lang.String" scope="request"/>
    <jsp:useBean id="isUserAllowedToMerge" type="java.lang.Boolean" scope="request"/>
    <jsp:useBean id="vulnerabilityVerificationEditable" type="java.lang.Boolean" scope="request"/>
    <core_rt:if test="${vulnerabilityVerificationEditable}">
        <jsp:useBean id="numberOfIncorrectVulnerabilities" type="java.lang.Long" scope="request"/>
    </core_rt:if>
    <jsp:useBean id="numberOfCheckedOrUncheckedVulnerabilities" type="java.lang.Long" scope="request"/>
    <jsp:useBean id="componentVisibilityRestriction" class="java.lang.Boolean" scope="request"/>
 </c:catch>

<%@include file="/html/utils/includes/logError.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">
    <core_rt:set var="inComponentDetailsContext" value="true" scope="request" />
    <%@include file="/html/components/includes/components/detailOverview.jspf"%>
</core_rt:if>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
