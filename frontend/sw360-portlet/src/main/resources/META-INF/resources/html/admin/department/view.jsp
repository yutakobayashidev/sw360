<%--
  ~ Copyright Siemens AG, 2021. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@ include file="/html/init.jsp" %>
<%--&lt;%&ndash; the following is needed by liferay to display error messages&ndash;%&gt;--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%--<jsp:useBean id='departmentIsScheduled' type="java.lang.Boolean" scope="request"/>--%>
<jsp:useBean id='departmentOffset' type="java.lang.String" scope="request"/>
<jsp:useBean id='departmentInterval' type="java.lang.String" scope="request"/>
<jsp:useBean id='departmentNextSync' type="java.lang.String" scope="request"/>
<portlet:defineObjects/>
<liferay-theme:defineObjects/>
<portlet:actionURL var="scheduleDepartmentURL" name="scheduleImportDepartment">
</portlet:actionURL>
<portlet:actionURL var="unscheduleDepartmentURL" name="unScheduleImportDepartment">
</portlet:actionURL>
<portlet:actionURL var="scheduleDepartmentManuallyURL" name="importDepartmentManually">
</portlet:actionURL>
<portlet:actionURL var="editPathFolder" name="writePathFolder">
</portlet:actionURL>
<portlet:resourceURL var="importDepartmentManually">
    <portlet:param name="<%=PortalConstants.ACTION%>"
                   value='<%=PortalConstants.IMPORT_DEPARTMENT_MANUALLY%>'/>
</portlet:resourceURL>
<%--<jsp:useBean id="departmentList" scope="request" class="java.util.HashMap"/>--%>
<%--<jsp:useBean id="allMessageError" scope="request" class="java.util.HashMap"/>--%>
<%--<jsp:useBean id="lastFileName" scope="request" class="java.lang.String"/>--%>
<%--<jsp:useBean id="pathConfigFolderDepartment" scope="request" class="java.lang.String"/>--%>
<%--<jsp:useBean id="lastRunningTime" scope="request" type="java.lang.String"/>--%>
<style>
    .error-none {
        display: none;
    }

    #content-${lastFileName} {
        display: block;
    }
</style>
<div class="container">
    <div class="row">
        <div class="col">
            <div class="row">
                <div class="col-6 portlet-toolbar">
                    <table class="table bordered-table">
                        <tr>
                            <th style="line-height: 40px"><liferay-ui:message key="registration.folder.path"/></th>
                            <td>
                                <form id="editPathFolder" name="editPathFolder" class="needs-validation"
                                      action="<%=editPathFolder%>" method="post" novalidate>
                                    <input id="pathFolderDepartment" style="margin-top: 0" required type="text"
                                           class="form-control"
                                           name="<portlet:namespace/><%=PortalConstants.DEPARTMENT_URL%>" value="<sw360:out value="${pathConfigFolderDepartment}"/>"
                                           placeholder="Enter the directory path"/>
                                </form>
                            </td>
                            <td width="3%">
                                <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message
                                        key="update"/></button>
                            </td>

                        </tr>
                        <tr>
                            <th><liferay-ui:message key="interval"/></th>
                            <td>${departmentInterval} (hh:mm:ss)</td>
                            <td></td>
                        </tr>
                        <tr>
                            <th><liferay-ui:message key="last.running.time.department"/></th>
                            <td>${lastRunningTime}</td>
                            <td></td>
                        </tr>
                        <tr>
                            <th><liferay-ui:message key="next.running.time.department"/></th>
                            <td>${departmentNextSync}</td>
                            <td></td>
                        </tr>
                    </table>
                    <form class="form mt-3">
                        <div class="form-group">
                            <button type="button" class="btn btn-primary"
                                    onclick="window.location.href='<%=scheduleDepartmentURL%>'"
                                    <core_rt:if test="${departmentIsScheduled}">disabled</core_rt:if> >
                                <liferay-ui:message key="schedule.department.service"/>
                            </button>
                            <button type="button" class="btn btn-light"
                                    onclick="window.location.href='<%=unscheduleDepartmentURL%>'"
                                    <core_rt:if test="${not departmentIsScheduled}">disabled</core_rt:if> >
                                <liferay-ui:message key="cancel.department.service"/>
                            </button>
                            <button type="button" class="btn btn-info" data-action="import-department-manually">
                                <liferay-ui:message key="manually"/>
                            </button>
                            <button type="button" class="btn btn-secondary" id="view-log"><liferay-ui:message
                                    key="view.log"/></button>
                        </div>
                    </form>
                </div>
            </div>
            <br>
            <br>
            <div class="row">
                <div class="col">
                    <h4 class="mt-1"><liferay-ui:message key="department"/></h4>
                    <table id="userTable" class="table table-bordered">
                        <thead>
                        <tr>
                            <th><liferay-ui:message key="department"/></th>
                            <th><liferay-ui:message key="member.emails"/></th>
                            <th width="5%"><liferay-ui:message key="actions"/></th>
                        </tr>
                        </thead>
                        <tbody>
                        <core_rt:forEach var="department" items="${departmentList}">
                            <tr>
                                <td style="text-align: center"><sw360:out value="${department.key}"/></td>
                                <td>
                                    <div style="width:100%; max-height:210px; overflow:auto">
                                        <core_rt:forEach var="secondDepartment" items="${department.value}"
                                                         varStatus="loop">
                                            <span>${loop.index + 1}.</span> <span><sw360:out
                                                value="${secondDepartment.email}"/></span>
                                            <br/>
                                        </core_rt:forEach>
                                    </div>
                                </td>
                                <td>
                                    <div class="actions" style="justify-content: center;">
                                        <svg class="editDepartment lexicon-icon">
                                            <title><liferay-ui:message key="edit"/></title>
                                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/>
                                        </svg>
                                        <svg class="lexicon-icon">
                                            <title><liferay-ui:message key="duplicate"/></title>
                                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#paste"/>
                                        </svg>
                                        <svg class="delete lexicon-icon">
                                            <title><liferay-ui:message key="delete"/></title>
                                            <use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>
                                        </svg>
                                    </div>
                                </td>
                            </tr>
                        </core_rt:forEach>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </div>
</div>

<div class="dialogs auto-dialogs">
    <div id="deleteComponentDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-xl modal-info" role="document">
            <div class="modal-content" style="width:100%; max-height:800px; overflow:auto">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <liferay-ui:message key="view.log"/>
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">
                    <div id="header-log-error">
                        <label for="file-log">File log</label>
                        <input list="file-logs" name="file-log" id="file-log"
                               class="col-sm-12 custom-select custom-select-sm"/>
                        <datalist id="file-logs">
                            <core_rt:forEach var="errorMessage" items="${allMessageError}">
                                <option value="${errorMessage.key}" ${errorMessage.key == lastFileName ? 'selected' : ''}>${errorMessage.key}</option>
                            </core_rt:forEach>
                        </datalist>
                    </div>
                    <hr>
                    <div id="content-log-error">
                        <core_rt:forEach var="errorMessage" items="${allMessageError}">
                            <div id="content-${errorMessage.key}" class="content-errors error-none">
                                <core_rt:forEach var="error" items="${errorMessage.value}">
                                    <p>${error}</p>
                                </core_rt:forEach>
                            </div>
                        </core_rt:forEach>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>
<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        require(['jquery', 'bridges/datatables', 'utils/includes/quickfilter', 'modules/dialog', 'modules/validation'], function ($, datatables, quickfilter, dialog, validation) {
            validation.enableForm('#editPathFolder');
            var usersTable;
            $('#view-log').on('click', showDialog);

            function showDialog() {
                $dialog = dialog.open('#deleteComponentDialog');
            }

            // initializing
            usersTable = createExistingUserTable('#userTable');

            function createExistingUserTable(tableSelector) {
                return datatables.create(tableSelector, {
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    columnDefs: [
                        {
                            "targets": 0,
                            "createdCell": function (td, cellData, rowData, row, col) {
                                $(td).attr('title', 'click the icon to toggle department text');
                            }
                        },
                        {
                            'targets': [2],
                            'orderable': false,
                        }
                    ],
                });
            }

            let progress = null;
            $('.portlet-toolbar button[data-action="import-department-manually"]').on('click', function () {
                var $dialog;
                if (progress != null) {
                    progress.abort();
                }

                function importDepartmentManually(callback) {
                    progress = $.ajax({
                        type: 'POST',
                        url: '<%=importDepartmentManually%>',
                        cache: false,
                        dataType: 'json'
                    }).always(function () {
                        callback();
                    }).done(function (data) {
                        $('.alert.alert-dialog').hide();
                        if (data.result === 'SUCCESS') {
                            $dialog.success(`<liferay-ui:message key="i.imported.x.out.of.y.department" />`);
                            location.reload();
                        } else if (data.result === 'PROCESSING') {
                            $dialog.info('<liferay-ui:message key="importing.process.is.already.running.please.try.again.later" />');
                        } else {
                            $dialog.alert('<liferay-ui:message key="error.happened.during.importing.some.department.may.not.be.imported" />');
                        }
                    }).fail(function () {
                        $('.alert.alert-dialog').hide();
                        $dialog.alert('<liferay-ui:message key="something.went.wrong" />');
                    });
                }

                $dialog = dialog.confirm(
                    null,
                    'question-circle',
                    '<liferay-ui:message key="import.department" />?',
                    '<p id="departmentConfirmMessage"><liferay-ui:message key="do.you.really.want.to.import.department" />',
                    '<liferay-ui:message key="import.department" />',
                    {},
                    function (submit, callback) {
                        $('#departmentConfirmMessage').hide();
                        $dialog.info('<liferay-ui:message key="importing.process.is.running.it.may.takes.a.few.minutes" />', true);
                        $('.modal-header > button').prop('disabled', false);
                        importDepartmentManually(callback);
                    }
                );
            });

            // // Check on input type change
            // $('#pathFolderDepartment').on('input', function () {
            //     if ($('#editPathFolder').hasClass('was-validated')) {
            //         validateInput();
            //     }
            // });
            //
            // // Check on enter key press
            // $('#pathFolderDepartment').keypress(function (event) {
            //     var keycode = (event.keyCode ? event.keyCode : event.which);
            //     if (keycode == '13' && !validateInput()) {
            //         event.preventDefault();
            //     }
            // });
            //
            $('.portlet-toolbar button[data-action="save"]').on('click', function (event) {
                $('#editPathFolder').submit();
            });
            //
            // function validateInput() {
            //     $('#editPathFolder').removeClass('needs-validation');
            //     $('#pathFolderDepartment')[0].setCustomValidity('');
            //     $('.invalid-feedback').css('display', 'none');
            //     $('.invalid-feedback').removeClass('d-block');
            //
            //     let pathFolderDepartment = $('#pathFolderDepartment').val();
            //
            //     if (pathFolderDepartment.length === 0 || $.trim(pathFolderDepartment).length === 0) {
            //         $('#editPathFolder').addClass('was-validated');
            //         $('#pathFolderDepartment')[0].setCustomValidity('error');
            //         $('#error-empty').addClass('d-block');
            //         return false;
            //     }
            //     ;
            //
            //     // const valid=/^[a-zA-Z]:\\(\w+\\)*\w*$/;
            //     // if(!pathFolderDepartment.match(valid)){
            //     //     $('#editPathFolder').addClass('was-validated');
            //     //     $('#pathFolderDepartment')[0].setCustomValidity('error');
            //     //     $('#error-invalid-char').addClass('d-block');
            //     //     return false;
            //     // };
            //
            //     return true;
            // }
        });
    });
    $('#file-log').on('change', function () {
        $('.content-errors').hide();
        $('#content-' + this.value).show();
    });

</script>

