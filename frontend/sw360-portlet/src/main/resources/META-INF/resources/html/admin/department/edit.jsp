<%@ taglib prefix="sw360" uri="http://example.com/tld/customTags.tld" %>
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

<%@ include file="/html/init.jsp" %>
<%@ include file="/html/utils/includes/logError.jspf" %>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf" %>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="com.liferay.portal.kernel.util.PortalUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User" %>
<jsp:useBean id="departmentKey" type="java.lang.String" scope="request"/>

<jsp:useBean id="departmentEmail" type="java.util.List<java.lang.String>" scope="request"/>
<jsp:useBean id="emails" type="java.util.List<java.lang.String>" scope="request"/>
<jsp:useBean id="departmentName" type="java.util.List<java.lang.String>" scope="request"/>
<jsp:useBean id="departmentList"
             type="java.util.HashMap<java.lang.String,org.eclipse.sw360.datahandler.thrift.users.User>"
             scope="request"/>
<core_rt:set var="addMode" value="${empty departmentKey}"/>

<portlet:resourceURL var="deleteDepartmentURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_DEPARTMENT_BY_EMAIL%>'/>
</portlet:resourceURL>



<portlet:actionURL var="updateURL" name="updateDepartment">
    <portlet:param name="<%=PortalConstants.DEPARTMENT_KEY%>" value="${departmentKey}"/>
</portlet:actionURL>


<div class="container">
    <div class="row">
        <div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group">
                            <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message
                                    key="update"/></button>
                        </div>
                        <div class="btn-group">
                            <button type="button" class="btn btn-light" data-action="cancel"><liferay-ui:message
                                    key="cancel"/></button>
                        </div>
                    </div>
                </div>
            </div>

            <div class="row">
                <div class="col">
                    <form id="departmentEditForm" name="departmentEditForm" action="<%=updateURL%>" method="post"
                          class="form needs-validation" novalidate>
                        <table id="departmentEdit" class="table edit-table three-columns" style="text-align: center">
<%--                            <thead>--%>
<%--                            <tr>--%>
<%--                                <th><liferay-ui:message key="department"/></th>--%>
<%--                                <th><liferay-ui:message key="member.emails"/></th>--%>
<%--                            </tr>--%>
<%--                            </thead>--%>
                            <tbody>
<%--                           <label > <b>Department</b></label><br><br>--%>
<%--                            <tr >--%>
<%--                                <sw360:out value="${departmentKey}"/>--%>
<%--                            </tr>--%>
<%--                            <br>--%>
<%--                            <br>--%>
<%--                            <br>--%>
<%--                            <label ><b>Member</b>Member</label><br>--%>
<%--                           <tr id="nameEmailDiv" >--%>
<%--                                <input style="display: none;" type="text" id="emailFake"--%>
<%--                                       name="<portlet:namespace/><%=PortalConstants.EMAIL_FAKE%>" value=""/>--%>
<%--&lt;%&ndash;                                <td><sw360:out value="${departmentKey}"/></td>&ndash;%&gt;--%>
<%--&lt;%&ndash;                                <td id="nameEmailDiv">&ndash;%&gt;--%>
<%--                                    <core_rt:forEach var="department" items="${departmentList}">--%>
<%--                                        <core_rt:forEach var="secondDepartment" items="${department.value}"--%>
<%--                                                         varStatus="loop">--%>
<%--                                            <div id="${loop.index}">--%>
<%--                                                <span>${loop.index + 1}.</span><span> <sw360:out--%>
<%--                                                    value="${secondDepartment.email}"/></span>--%>

<%--                                                &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;--%>
<%--                                                <svg class="delete lexicon-icon"--%>
<%--                                                     data-map="${secondDepartment.email}"--%>
<%--                                                     data-id="${loop.index}">--%>
<%--                                                    <title><liferay-ui:message key="delete"/></title>--%>
<%--                                                    <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>--%>
<%--                                                </svg>--%>
<%--                                            </div>--%>
<%--                                        </core_rt:forEach>--%>
<%--                                    </core_rt:forEach>--%>
<%--                                    </br>--%>
<%--                                    <div>--%>
<%--                                        <%@include file="/html/utils/includes/addUser.jsp" %>--%>
<%--                                    </div>--%>
<%--&lt;%&ndash;                                </td>&ndash;%&gt;--%>
<%--                            </tr>--%>


<%--                            code clean--%>
<%--                            <tr>--%>
                                <input style="display: none;" type="text" id="emailFake"
                                       name="<portlet:namespace/><%=PortalConstants.EMAIL_FAKE%>" value=""/>
                                <tr ><sw360:out value="${departmentKey}"/></tr>
                                <br/>
                                <br/>
                                <label>Email </label>
                                <tr >
                                    <table id="nameEmailDiv" cellspacing="0" cellpadding="0" >
                                        <core_rt:forEach var="department" items="${departmentList}">
                                            <div style="width:100%; max-height:210px; overflow:auto">
                                        <core_rt:forEach var="secondDepartment" items="${department.value}" varStatus="loop">
                                        <tr id="${loop.index}">
                                            <td >

                                                        <span>${loop.index + 1}.</span><span> <sw360:out
                                                            value="${secondDepartment.email}"/></span>

                                            </td>
                                            <td>
                                                <svg class="delete lexicon-icon"
                                                     data-map="${secondDepartment.email}"
                                                     data-id="${loop.index}" >
                                                    <title><liferay-ui:message key="delete"/></title>
                                                    <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                                                </svg>
                                            </td>
                                        </tr>
                                        </core_rt:forEach>
                                            </div>
                                        </core_rt:forEach>
                                    </table>
                                    </br>
                                    <div>
                                        <%@include file="/html/utils/includes/addUser.jsp" %>
                                    </div>
                                </tr>
<%--                 </td>--%>
<%--                            </tr>--%>


                            </tbody>
                        </table>
                    </form>
                </div>
            </div>


        </div>
    </div>
</div>


<div class="dialogs auto-dialogs"></div>

<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%--<script>--%>
<script >
    AUI().use('liferay-portlet-url', function () {

        require(['jquery', 'modules/dialog', 'modules/validation'], function ($, dialog, validation) {
            pageName = '<%=PortalConstants.PAGENAME%>';
            pageEdit = '<%=PortalConstants.PAGENAME_EDIT%>';

            validation.enableForm('#departmentEditForm');

            $('.portlet-toolbar button[data-action="cancel"]').on('click', function () {
                var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
                var portletURL = Liferay.PortletURL.createURL(baseUrl)
                <core_rt:if test="${not addMode}" >
                    .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_DETAIL%>')
                    </core_rt:if>
                    <core_rt:if test="${addMode}" >
                    .setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_VIEW%>')
                    </core_rt:if>
                    .setParameter('<%=PortalConstants.DEPARTMENT_KEY%>', '${departmentKey}');

                window.location = portletURL.toString();
            });

            $('.portlet-toolbar button[data-action="save"]').on('click', function () {
                $('#departmentEditForm').submit();
            });

            $('#nameEmailDiv').on('click', 'svg.delete', function (event) {
                var data = $(event.currentTarget).data();
                var id = data.id;
                deleteDepartment(data.map, id);

            });

            function deleteDepartment(email, id) {
                var $dialog;
                function deleteDepartmentInternal(callback) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=deleteDepartmentURL%>',
                        cache: false,
                        data: {
                            <portlet:namespace/>email: email,
                        },
                        success: function (data) {
                            callback();
                            console.log(data);
                            if (data.result == 'SUCCESS') {
                                console.log(data);
                                $('#nameEmailDiv #' + id).remove();

                                $dialog.close();
                            } else if (data.result == 'ACCESS_DENIED') {
                                $dialog.alert('<liferay-ui:message key="do.you.really.want.to.delete.user.x" />');
                            } else {
                                $dialog.alert("<liferay-ui:message key="i.could.not.delete.user" />");

                            }
                            setInterval('location.reload()', 100);
                        },
                        error: function () {
                            callback();
                            $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.vendor" />');
                        }

                    });
                }

                $dialog = dialog.confirm(

                    'danger',
                    'question-circle',
                    '<liferay-ui:message key="delete.vendor" /> ?',
                    '<p><liferay-ui:message key="do.you.really.want.to.delete.the.vendor.x" />?</p>',
                    '<liferay-ui:message key="delete.vendor" />',
                    {
                        email: email,
                    },
                    function (submit, callback) {
                        deleteDepartmentInternal(callback);

                    }
                );
                // window.location.reload();
            }
        });
    });


</script>
