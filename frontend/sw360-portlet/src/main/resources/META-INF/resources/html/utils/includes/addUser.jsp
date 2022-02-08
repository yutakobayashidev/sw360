<%--
  ~ Copyright Siemens AG, 2017-2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
--%>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.users.User" %>


<table class="table edit-table two-columns-with-actions" id="secDepartmentRolesTable">
    <thead>
    <tr>
        <th colspan="1" class="headlabel"><liferay-ui:message key="Add Email" /></th>
    </tr>
    </thead>
</table>

<button type="button" class="btn btn-secondary" id="add-sec-grp-roles-id">
    <liferay-ui:message key="Add" />
</button>

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


<script>
    require(['jquery', 'modules/dialog'], function($, dialog) {
        addRowToSecDepartmentRolesTable();
        $('#add-sec-grp-roles-id').on('click', function() {
            addRowToSecDepartmentRolesTable();

        });
        $('#secDepartmentRolesTable').on('click', 'svg[data-row-id]', function(event) {
            var rowId = $(event.currentTarget).data().rowId;

            dialog.open('#deleteSecGrpRolesDialog', {
                text: "<liferay-ui:message key="do.you.really.want.to.remove.this.item" />",
            }, function(submit, callback) {
                $('#' + rowId).remove();
                callback(true);
            });
        });

        function addRowToSecDepartmentRolesTable(key, value, rowId) {
            if (!rowId) {
                rowId = "secDepartmentRolesTableRow" + Date.now();
            }
            if ((!key) && (!value)) {
                key = "";
                value = "";
            }
            var newRowAsString =
                '<tr id="' + rowId + '" class="bodyRow">' +
                '<td>' +
                '<input list="grpsKeyList" class="form-control" id="myInput" name="<portlet:namespace/><%=PortalConstants.ADD_LIST_EMAIL%>"  required="" minlength="1"  placeholder="<liferay-ui:message key="Search User" />" title="<liferay-ui:message key="select.secondary.department.role" />"  value="" />'+
                ' <datalist id="grpsKeyList">'+
                '<core_rt:forEach var="email" items="${emails}" varStatus="loop"> '+
                '<option value="${email}">${email}</option>--%> '+
                 '</core_rt:forEach> '+
                '</datalist>'+
                '<p id="result"></p>'+
                 prepareKeyDatalist() +
                 '</td>' +
                '<td class="content-middle">' +
                '<svg class="action lexicon-icon" data-row-id="' + rowId + '">' +
                '<title>Delete</title>' +
                '<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>' +
                '</svg>' +
                '</td>' +
                '</tr>';
            $('#secDepartmentRolesTable tr:last').after(newRowAsString);
            $("#myInput" + rowId + " option").each(function() {
                if ($(this).val() === value) {
                    $(this).attr('selected', 'selected');
                    return false;
                }
            });

        }
        function prepareKeyDatalist() {
            var datalist = '<datalist id="grpsKeyList">';
            <core_rt:forEach items="${grpsKeys}" var="grpsKey">
            datalist += '<option value="' + "${grpsKey}" + '">';
            </core_rt:forEach>
            return datalist + '</datalist>';
        }

        $(document).ready(function(){
            $("#addEmail").after(prepareKeyDatalist()).attr("list","grpsKeyList")
        })



        const validate = () => {
            const $result = $('#result');
            const email = $('#myInput').val();
            $result.text('');

            if (!validateEmail(email)) {
                $result.text(email + ' is not valid :(');
                $result.css('color', 'red');
            }
            return false;
        }
        const validateEmail = (email) => {
            return email.match(
                /^(([^<>()[\]\\.,;:\s@\"]+(\.[^<>()[\]\\.,;:\s@\"]+)*)|(\".+\"))@((\[[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}\])|(([a-zA-Z\-0-9]+\.)+[a-zA-Z]{2,}))$/
            );
        };

        $('#myInput').on('input', validate);

    });

</script>
