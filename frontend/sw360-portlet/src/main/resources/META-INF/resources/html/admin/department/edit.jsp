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

<jsp:useBean id="departmentEncode" type="java.lang.String" scope="request"/>
<jsp:useBean id="departmentRoleUser" type="java.lang.String" scope="request"/>
<jsp:useBean id="emailOtherDepartment" type="java.lang.String" scope="request"/>
<%--<jsp:useBean id="departmentEmail" type="java.util.List<java.lang.String>" scope="request"/>--%>
<%--<jsp:useBean id="emails" type="java.util.List<java.lang.String>" scope="request"/>--%>
<%--<jsp:useBean id="departmentName" type="java.util.List<java.lang.String>" scope="request"/>--%>
<%--<jsp:useBean id="departmentList"--%>
<%--             type="java.util.HashMap<java.lang.String,org.eclipse.sw360.datahandler.thrift.users.User>"--%>
<%--             scope="request"/>--%>

<%--<core_rt:set var="addMode" value="${empty departmentKey}"/>--%>

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
                                    key="Update Department"/></button>
                        </div>
                        <div class="btn-group">
                            <button type="button" class="btn btn-light" data-action="cancel"><liferay-ui:message
                                    key="Cancel"/></button>
                        </div>
                    </div>
                </div>
            </div>
            <div class="row">
                <div class="col">
                    <form id="departmentEditForm" name="departmentEditForm" action="<%=updateURL%>" method="post" class="form needs-validation" novalidate>
                        <table class="table edit-table two-columns-with-actions" id="secDepartmentRolesTable">
                            <thead>
                            <input style="display: none;" type="text" id="listEmail" name="<portlet:namespace/><%=PortalConstants.ADD_LIST_EMAIL%>" value="" />
                            <tr>
                                <th colspan="3" class="headlabel" ><liferay-ui:message key="Edit Department"/>   ${departmentEncode}</th>
                            </tr>
                            </thead>
                            <tbody>
                                <tr id="" class="bodyRow" display="none">
                                    <td>
                                        <input list="suggestionsList" class="form-control secGrp" name="email" placeholder="<liferay-ui:message key="Search User" />" title="<liferay-ui:message key="select.secondary.department.role" />"   />
                                        <datalist class="suggestion" id="suggestionsList">
                                        </datalist>
                                    </td>
                                    <td>
                                        <input  class="form-control" disabled class="secGrp" minlength="1" placeholder="<liferay-ui:message key="role" />" title="<liferay-ui:message key="role" />" value="User"/>
                                    </td>
                                    <td class="content-middle">
                                        <svg class="action lexicon-icon delete-btn" data-value="" data-row-id="" onclick="">
                                            <title>Delete</title>
                                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                                        </svg>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <button type="button" class="btn btn-secondary" id="add-sec-grp-roles-id">
                            <liferay-ui:message key="Add User" />
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</div>
<div class="dialogs">
    <div id="deleteSecGrpRolesDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <clay:icon symbol="question-circle" />
                        <liferay-ui:message key="delete.item" />
                        ?
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <p data-name="text"></p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal">
                        <liferay-ui:message key="cancel" />
                    </button>
                    <button type="button" class="btn btn-danger">
                        <liferay-ui:message key="delete.item" />
                    </button>
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
            let index=0;
            let emailsAdd=[];
            let emailJSON = jQuery.parseJSON(JSON.stringify(${ departmentRoleUser }));
            let emailOtherDepartment= jQuery.parseJSON(JSON.stringify(${emailOtherDepartment}));
            createSecDepartmentRolesTable();
            pageName = '<%=PortalConstants.PAGENAME%>';
            pageEdit = '<%=PortalConstants.PAGENAME_EDIT%>';
            validation.enableForm('#departmentEditForm');

            $('.portlet-toolbar button[data-action="cancel"]').on('click', function () {
                var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
                var portletURL = Liferay.PortletURL.createURL(baseUrl).setParameter('<%=PortalConstants.PAGENAME%>', '<%=PortalConstants.PAGENAME_VIEW%>');
                window.location = portletURL.toString();
            });

            $('.portlet-toolbar button[data-action="save"]').on('click', function () {
                $('.secGrp').each(function() {
                    emailsAdd.push($(this).val())
                });
                var jsonArrayEmail = JSON.parse(JSON.stringify(emailsAdd));
                console.log("--------------"+jsonArrayEmail);
                $('#listEmail').val(JSON.stringify(jsonArrayEmail));
                $('#departmentEditForm').submit();
            });

            function createSecDepartmentRolesTable() {
                $('.delete-btn').first().bind('click', deleteRow);

                if (emailJSON.length == 0) {
                    return;                    
                }

                for (let i = 0; i < emailJSON.length - 1; i++) {
                    addNewRow();
                }

                for (let i = 0; i < emailJSON.length; i++) {
                    $('.secGrp').eq(i).val(emailJSON[i].Email);
                }

                fillSuggestion();
            }

            $('#add-sec-grp-roles-id').on('click', function() {
                addNewRow();
            });

            function addNewRow() {
                if ($('.bodyRow').first().css('display') == 'none') {
                    $('.bodyRow').last().css('display', 'table-row');
                    return;
                }

                let newRow = $('.bodyRow').last().clone();

                $('#secDepartmentRolesTable').find('tbody').append(newRow);

                $('.secGrp').last().val('');

                $('.delete-btn').last().bind('click', deleteRow);
            }

            function deleteRow() {
                let email = $(this).parent().parent().children('td').first().children('input').val();
    
                let emailObject = { Email: email };
    
                emailOtherDepartment.push(emailObject);
                emailOtherDepartment = Array.from(new Set(emailOtherDepartment));
    
                if ($('.delete-btn').length > 1) {
                    $(this).closest('tr').remove();
                } else {
                    $('.secGrp').val('');
                    $(this).closest('tr').css('display', 'none');
                }
    
                fillSuggestion();
            }

            function fillSuggestion() {
                let suggestionsList = '';

                for(let email of emailOtherDepartment) {
                    suggestionsList += '<option value="'+email.Email+'">' + email.Email + '</option>';
                }

                $('.suggestion').empty();

                $('.suggestion').each(function() {
                    $(this).html(suggestionsList);
                });
            }

            function handleFocusOut(element) {
                let value = element.val();
                for (let i = 0; i < emailOtherDepartment.length; i++) {
                    if (emailOtherDepartment[i].Email == value) {
                        $(element).val(emailOtherDepartment[i].Email);
                        break;
                    } else {
                        $(element).val("");
                        continue;
                    }
                }
            }
        });
    });
</script>
