<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@include file="/html/init.jsp"%>
<%-- the following is needed by liferay to display error messages--%>
<%@include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<portlet:defineObjects />
<liferay-theme:defineObjects />

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.util.PortalUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.vendors.Vendor" %>


<jsp:useBean id="vendor" class="org.eclipse.sw360.datahandler.thrift.vendors.Vendor" scope="request" />

<jsp:useBean id="releaseList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.components.Release>"  scope="request"/>
<jsp:useBean id="documentID" class="java.lang.String" scope="request" />

<core_rt:set  var="addMode"  value="${empty vendor.id}" />

<portlet:actionURL var="updateURL" name="updateVendor">
    <portlet:param name="<%=PortalConstants.VENDOR_ID%>" value="${vendor.id}" />
</portlet:actionURL>

<portlet:actionURL var="deleteVendorURL" name="removeVendor">
    <portlet:param name="<%=PortalConstants.VENDOR_ID%>" value="${vendor.id}"/>
</portlet:actionURL>


<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
                        <core_rt:if test="${addMode}" >
                            <div class="btn-group">
                                <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="create.vendor" /></button>
                            </div>
                        </core_rt:if>
						<core_rt:if test="${not addMode}" >
                            <div class="btn-group">
                                <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="update.vendor" /></button>
                            </div>
                            <div class="btn-group">
                                <button type="button" class="btn btn-danger" data-action="delete" data-vendor-name="<sw360:out value="${vendor.fullname}"/>"><liferay-ui:message key="delete.vendor" /></button>
						    </div>
                        </core_rt:if>
                        <div class="btn-group">
                            <button type="button" class="btn btn-light" data-action="cancel"><liferay-ui:message key="cancel" /></button>
                        </div>
					</div>
				</div>
                <div class="col portlet-title text-truncate" title="<sw360:out value="${vendor.fullname}"/>">
					<sw360:out value="${vendor.fullname}"/>
				</div>
            </div>

            <div class="row">
                <div class="col">
                    <form id="vendorEditForm" name="vendorEditForm" action="<%=updateURL%>" method="post" class="form needs-validation" novalidate>
                        <table id="VendorEdit" class="table edit-table three-columns">
                            <thead>
                                <tr>
                                    <th colspan="3"><liferay-ui:message key="edit.vendor" /></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td>
                                        <div class="form-group">
                                            <label for="vendorFullname"><liferay-ui:message key="full.name" /></label>
                                            <input id="vendorFullname" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.vendor.fullname" />" name="<portlet:namespace/><%=Vendor._Fields.FULLNAME%>"
                                                value="<sw360:out value="${vendor.fullname}"/>" />
                                             <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.a.full.name" />
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label for="vendorShortname"><liferay-ui:message key="short.name" /></label>
                                            <input id="vendorShortname" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.vendor.short.name" />"
                                                   name="<portlet:namespace/><%=Vendor._Fields.SHORTNAME%>"
                                                value="<sw360:out value="${vendor.shortname}"/>" />

                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.a.short.name" />
                                            </div>
                                        </div>
                                    </td>
                                    <td>
                                        <div class="form-group">
                                            <label for="vendorURL"><liferay-ui:message key="url" /></label>
                                            <input id="vendorURL" type="url" required class="form-control" placeholder="<liferay-ui:message key="enter.vendor.url" />" name="<portlet:namespace/><%=Vendor._Fields.URL%>"
                                                value="<sw360:out value="${vendor.url}"/>" />
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.an.url" />
                                            </div>
                                        </div>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                    </form>
                </div>
            </div>

            <div class="row">
                <div class="col">
                    <core_rt:if test="${releaseList.size() > 0}" >
                        <h4 class="mt-4"><liferay-ui:message key="used.by.the.following.releases" /></h4>
                        <table class="table bordered-table">
                            <tbody>
                                <tr>
                                    <core_rt:forEach var="release" items="${releaseList}" varStatus="loop">
                                        <td><sw360:DisplayReleaseLink release="${release}"/></td>
                                        <core_rt:if test="${loop.count > 0 and  loop.count %  4 == 0}">
                                            </tr><tr>
                                        </core_rt:if>
                                    </core_rt:forEach>
                                </tr>
                            </tbody>
                        </table>
                    </core_rt:if>
                </div>
            </div>
		</div>
	</div>
</div>

<div class="dialogs auto-dialogs"></div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    require(['jquery', 'modules/dialog', 'modules/validation' ], function($, dialog, validation) {

        validation.enableForm('#vendorEditForm');

        $('.portlet-toolbar button[data-action="cancel"]').on('click', function() {
            var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
            var portletURL = Liferay.PortletURL.createURL( baseUrl )
                    <core_rt:if test="${not addMode}" >
                    .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_DETAIL%>')
                    </core_rt:if>
                    <core_rt:if test="${addMode}" >
                    .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_VIEW%>')
                    </core_rt:if>
                    .setParameter('<%=PortalConstants.VENDOR_ID%>','${vendor.id}');
            window.location = portletURL.toString();
        });

        $('.portlet-toolbar button[data-action="delete"]').on('click', function(event) {
            var data = $(event.currentTarget).data();

            dialog.confirm(
                'danger',
                'question-circle',
                '<liferay-ui:message key="delete.vendor" />?',
                '<p><liferay-ui:message key="do.you.really.want.to.delete.the.vendor.x" />?</p>',
                '<liferay-ui:message key="delete.vendor" />',
                {
                    name: data.vendorName,
                },
                function(submit, callback) {
                    window.location.href = '<%=deleteVendorURL%>';
                }
            );
        });

        $('.portlet-toolbar button[data-action="save"]').on('click', function() {
            $('#vendorEditForm').submit();
        });
    });
</script>
