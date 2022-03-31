<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@include file="/html/init.jsp" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.components.ReleaseLink" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ProjectReleaseRelationship" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.ReleaseRelationship" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.MainlineState" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.ReleaseLink>"  scope="request"/>
<core_rt:set var="mainlineStateEnabledForUserRole" value='<%=PortalConstants.MAINLINE_STATE_ENABLED_FOR_USER%>'/>
<core_rt:forEach items="${releaseList}" var="releaseLink" varStatus="loop">

    <core_rt:choose>
        <core_rt:when test="${releaseLink.accessible}">

            <core_rt:set var="uuid" value="${releaseLink.id}"/>
            <tr id="releaseLinkRow${uuid}" >
                <td>
                    <div class="form-group">
                        <input type="hidden" value="${releaseLink.id}" name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ReleaseLink._Fields.ID%>">
                        <input id="releaseName" type="text" placeholder="<liferay-ui:message key="enter.release" />" class="form-control"
                            value="<sw360:out value="${releaseLink.name}"/>" readonly/>
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <input id="releaseVersion" type="text" placeholder="<liferay-ui:message key="enter.version" />" class="form-control"
                            value="<sw360:out value="${releaseLink.version}"/>" readonly/>
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <select id="releaseRelation"
                                name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ProjectReleaseRelationship._Fields.RELEASE_RELATION%>"
                                class="form-control">
                            <sw360:DisplayEnumOptions type="<%=ReleaseRelationship.class%>" selected="${releaseLink.releaseRelationship}"/>
                        </select>
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <select class="form-control" id="mainlineState"
                                name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ProjectReleaseRelationship._Fields.MAINLINE_STATE%>"
                                <core_rt:if test="${not isUserAtLeastClearingAdmin and not mainlineStateEnabledForUserRole}" >
                                    disabled="disabled"
                                </core_rt:if>
                        >
                            <sw360:DisplayEnumOptions type="<%=MainlineState.class%>" selected="${releaseLink.mainlineState}"/>
                        </select>
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <input id="releaseComment" name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ProjectReleaseRelationship._Fields.COMMENT%>"
                        type="text" placeholder="<liferay-ui:message key="enter.comment" />" class="form-control"
                            value="<sw360:out value="${releaseLink.comment}"/>"/>
                    </div>
                </td>
                <td class="content-middle">
                    <svg class="action lexicon-icon" data-row-id="releaseLinkRow${uuid}" data-release-name="<sw360:out value='${releaseLink.longName}' jsQuoting="true"/>">
                        <title><liferay-ui:message key="delete" /></title>
                        <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                    </svg>
                </td>
            </tr>
    
        </core_rt:when>
        <core_rt:otherwise>

            <tr id="releaseLinkRow${loop.count}" >
                <td>
                    <liferay-ui:message key="inaccessible.release" />                
                    <div class="form-group">
                        <input type="hidden" value="${releaseLink.id}" name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ReleaseLink._Fields.ID%>">
                    </div>
                </td>
                <td>
                </td>
                <td>
                    <div class="form-group">
                        <input id="releaseRelation" type="hidden" 
                                name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ProjectReleaseRelationship._Fields.RELEASE_RELATION%>"
                                value="${releaseLink.releaseRelationship.getValue()}"
                                >
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <input id="mainlineState" type="hidden" 
                                name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ProjectReleaseRelationship._Fields.MAINLINE_STATE%>"
                                value="${releaseLink.mainlineState.getValue()}"
                                >
                    </div>
                </td>
                <td>
                    <div class="form-group">
                        <input id="releaseComment" type="hidden" 
                                name="<portlet:namespace/><%=Project._Fields.RELEASE_ID_TO_USAGE%><%=ProjectReleaseRelationship._Fields.COMMENT%>"
                                value="<sw360:out value="${releaseLink.comment}"/>"
                                >
                    </div>
                </td>
                <td class="content-middle">
                </td>
            </tr>

        </core_rt:otherwise>
    </core_rt:choose>
</core_rt:forEach>
