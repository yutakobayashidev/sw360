<%--
  ~ Copyright Siemens AG, 2013-2017, 2019. Part of the SW360 Portal Project.
  ~
  ~ This program and the accompanying materials are made
  ~ available under the terms of the Eclipse Public License 2.0
  ~ which is available at https://www.eclipse.org/legal/epl-2.0/
  ~
  ~ SPDX-License-Identifier: EPL-2.0
  --%>

<%@ page import="org.eclipse.sw360.portal.common.PortalConstants"%>

<%@include file="/html/init.jsp"%>

<portlet:defineObjects />
<liferay-theme:defineObjects />
<portlet:resourceURL var="evaluateCLIAttachments">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.EVALUATE_CLI_ATTACHMENTS%>"/>
    <portlet:param name="<%=PortalConstants.RELEASE_ID%>" value="${releaseId}"/>
</portlet:resourceURL>
<core_rt:catch var="attributeNotFoundException">
    <jsp:useBean id="attachments" type="java.util.Set<org.eclipse.sw360.datahandler.thrift.attachments.Attachment>" scope="request" />
    <jsp:useBean id="attachmentUsages" type="java.util.Map<java.lang.String, java.util.List<org.eclipse.sw360.datahandler.thrift.projects.Project>>" scope="request" />
    <jsp:useBean id="attachmentUsagesRestrictedCounts" type="java.util.Map<java.lang.String, java.lang.Long>" scope="request" />
    <jsp:useBean id="documentType" type="java.lang.String" scope="request" />
    <jsp:useBean id="documentID" class="java.lang.String" scope="request" />
</core_rt:catch>

<%--for javascript library loading --%>
<%@ include file="/html/utils/includes/requirejs.jspf" %>

<core_rt:if test="${empty attributeNotFoundException}">

    <core_rt:if test="${empty attachments}">
        <div class="alert alert-info" role="alert">
            <liferay-ui:message key="no.attachments.yet" />
        </div>

        <script>
            require(['jquery'], function($) {
                $('#downloadAttachmentBundle').hide();
            });
        </script>
    </core_rt:if>

    <core_rt:if test="${not empty attachments}">
        <core_rt:if test="${not empty releaseId and writeAccessUser}">
          <div id="errorMsgContainer">
            <div id="errorMsg" class="alert alert-dismissible d-none" role="alert">
                <span></span>
                <button type="button" class="close" data-dismiss="alert" aria-label="Close">
                    <span aria-hidden="true">&times;</span>
                </button>
            </div>
          </div>
        </core_rt:if>
        <table id="attachmentsDetail" class="table table-bordered" title="<liferay-ui:message key="attachment.information" />">
            <colgroup>
                <col />  <!-- set by class -->
                <col style="width: 25%;" />
                <col style="width: 10%;" />
                <col style="width: 8%;" />
                <col style="width: 8%;" />
                <col style="width: 16%;" />
                <col style="width: 11%;" />
                <col style="width: 16%;" />
                <col style="width: 6%;" />
                <col /> <!-- set by class -->
            </colgroup>
            <thead>
                <tr>
                    <th class="one action">
                        <sw360:DisplayDownloadAttachmentBundle attachments="${attachments}"
                            name="AttachmentBundle.zip"
                            contextType="${documentType}"
                            contextId="${documentID}" />
                    </th>
                    <th><liferay-ui:message key="file.name" /></th>
                    <th><liferay-ui:message key="size" /></th>
                    <th><liferay-ui:message key="type" /></th>
                    <th><liferay-ui:message key="group" /></th>
                    <th><liferay-ui:message key="uploaded.by" /></th>
                    <th><liferay-ui:message key="group" /></th>
                    <th><liferay-ui:message key="checked.by" /></th>
                    <th><liferay-ui:message key="usages" /></th>
                    <th><liferay-ui:message key="actions" /></th>
                </tr>
            </thead>
            <tbody>
            </tbody>
        </table>

        <script>
            require(['jquery', 'bridges/datatables', 'modules/dialog' ], function($, datatables, dialog) {
                var attachmentJSON = [];
                var usageLinks;

                /* Print all attachment table data as array into the html page */
                <core_rt:forEach items="${attachments}" var="attachment">
                    usageLinks = [];
                    <core_rt:forEach items="${attachmentUsages[attachment.attachmentContentId]}" var="project">
                    usageLinks.push("<sw360:DisplayProjectLink project="${project}"/>");
                    </core_rt:forEach>
                    attachmentJSON.push({
                        "fileName": "<sw360:out value="${attachment.filename}"/>",
                        "size": "n/a",
                        "type": "<sw360:DisplayEnumShort value="${attachment.attachmentType}"/>",
                        "uploadedTeam": "<sw360:DisplayEllipsisString value="${attachment.createdTeam}"/>",
                        "uploadedBy": "<sw360:DisplayEllipsisString value="${attachment.createdBy}"/>",
                        "checkedTeam":  "<sw360:DisplayEllipsisString value="${attachment.checkedTeam}"/>",
                        "checkedBy":  "<sw360:DisplayEllipsisString value="${attachment.checkedBy}"/>",
                        "usage":  {links: usageLinks, restrictedCount: ${attachmentUsagesRestrictedCounts.getOrDefault(attachment.attachmentContentId, 0)}},
                        "actions":     "<div class=\"actions\"><sw360:DisplayDownloadAttachmentFile attachment="${attachment}" contextType="${documentType}" contextId="${documentID}"/></div>",
                        "sha1": "<sw360:out value="${attachment.sha1}"/>",
                        "uploadedOn": "<sw360:out value="${attachment.createdOn}"/>",
                        "uploadedComment": "<core_rt:if test="${not empty attachment.createdComment}">Comment: <sw360:DisplayEllipsisString value="${attachment.createdComment}"/></core_rt:if>",
                        "checkedOn": "<sw360:out value="${attachment.checkedOn}"/>",
                        "checkedComment": "<core_rt:if test="${not empty attachment.checkedComment}">Comment: <sw360:DisplayEllipsisString value="${attachment.checkedComment}"/></core_rt:if>",
                        "checkStatus": "<sw360:out value="${attachment.checkStatus}"/>",
                        "attachmentContentId": "<sw360:out value="${attachment.attachmentContentId}"/>",
                        "superAttachmentId": "<sw360:out value="${attachment.superAttachmentId}"/>",
                        "superAttachmentFilename": "<sw360:out value="${attachment.superAttachmentFilename}"/>"
                    });
                </core_rt:forEach>

                /* register listener to toolbar button */
                $('#downloadAttachmentBundle').on('click', function() {
                    window.location = $('#attachmentsDetail th:first a').attr('href');
                });


                /* create table */
                var table = datatables.create('#attachmentsDetail', {
                    "data": attachmentJSON,
                    "columns": [
                        {
                            "className":      'one action details-control',
                            "orderable":      false,
                            "data":           null,
                            "defaultContent": ''
                        },
                        { "data": "fileName" , "render": renderAttachmentFileName},
                        { "data": "size" },
                        { "data": "type" },
                        { "data": "uploadedTeam" },
                        { "data": "uploadedBy" },
                        { "data": "checkedTeam" },
                        { "data": "checkedBy" },
                        { "data": "usage", "render": renderAttachmentUsages, orderable: false, className: 'text-center' },
                        { "data": "actions", className: "one action", orderable: false }
                    ],
                    "columnDefs": [
                        {
                            "targets": [ 6, 7 ],
                            "createdCell": function (td, cellData, rowData, row, col) {
                                if (rowData.checkStatus === 'REJECTED') {
                                    $(td).addClass('text-danger');
                                } else if (rowData.checkStatus === 'ACCEPTED') {
                                    $(td).addClass('text-success');
                                }

                                if (rowData.superAttachmentId) {
                                    $(td).addClass('text-white').css("background-color", "#adaec1 !important");
                                }
                            }
                        },
                        {
                            "targets": [8],
                            "createdCell": function (td, cellData, rowData, row, col) {
                                $(td).on('click', 'a', function() {
                                    var dialogContent = '';
                                    dialogContent += rowData.usage.links.join(", ");
                                    if (rowData.usage.restrictedCount > 0){
                                        if (rowData.usage.links.length > 0) {
                                            dialogContent += ", and ";
                                        }
                                        dialogContent += rowData.usage.restrictedCount + " restricted project(s)";
                                    }
                                    dialog.info(
                                        'Projects using this attachment',
                                        dialogContent
                                    );
                                });

                                if (rowData.superAttachmentId) {
                                    $(td).addClass('text-white').css("background-color", "#adaec1 !important");
                                }
                            }
                        },
                        {
                            "targets": [ 0, 1, 2, 3, 4 ,5, 9 ],
                            "createdCell": function (td, cellData, rowData, row, col) {
                                if (rowData.superAttachmentId) {
                                    $(td).addClass('text-white').css("background-color", "#adaec1 !important");
                                }
                            }
                        }
                    ],
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    "order": [[1, 'asc']]
                    <core_rt:if test="${not empty releaseId and writeAccessUser}">
                    ,"buttons": [
                      {
                        text: '<liferay-ui:message key="evaluate.cli.files" />',
                        "className": 'btn btn-primary',
                        "attr": {
                            "id": 'evaluateCLIFiles',
                         },
                        "action": evaluateCLIFiles
                      }
                    ]
                    </core_rt:if>
                } );

                /* Add event listener for opening and closing details as child row */
                $('#attachmentsDetail tbody').on('click', 'td.details-control', function () {
                    var tr = $(this).closest('tr');
                    var row = table.row( tr );

                    if ( row.child.isShown() ) {
                        row.child.hide();
                        tr.removeClass('shown');
                    } else {
                        row.child( createChildRow(row.data()) ).show();
                        tr.addClass('shown');
                    }
                } );

                /* Define function for child row creation, which will contain additional data for a clicked table row */
                function createChildRow(rowData) {
                    let className = "";
                    if (rowData.checkStatus === 'ACCEPTED') {
                        className = "foregroundOK";
                          } else if (rowData.checkStatus === 'REJECTED') {
                        className = "foregroundAlert";
                        }
                    var childHtmlString = `
                    <table class="table table-borderless"
                         <tr class="dataTableChildRowCell">
                             <td><liferay-ui:message key="sha" /> : </td>
                             <td>`+ rowData.sha1 + `</td>
                             <td><liferay-ui:message key="uploaded.on" /> : </td>
                             <td>` + rowData.uploadedOn + `</td>
                             <td><liferay-ui:message key="uploaded.comment" /> : </td>
                             <td>` + rowData.uploadedComment + `</td>
                         <tr>
                         <tr class="dataTableChildRowCell ` + className + `">
                             <td><liferay-ui:message key="checked.on" /> : </td>
                             <td>` + rowData.checkedOn + `</td>
                             <td><liferay-ui:message key="checked.comment" /> : </td>
                             <td>` + rowData.checkedComment + `</td>
                             <td></td>
                             <td></td>
                         <tr>`

                    if (rowData.superAttachmentId) {
                        childHtmlString += `<tr class="dataTableChildRowCell">
                             <td><liferay-ui:message key="super.attachment.id" /> : </td>
                             <td>` + rowData.superAttachmentId + `</td>
                             <td><liferay-ui:message key="super.attachment.filename" /> : </td>
                             <td>`+ rowData.superAttachmentFilename + `</td>
                             <td></td>
                             <td></td>
                             <tr>`
                    }
                    childHtmlString += '<table>'
                    return childHtmlString;
                }

                function renderAttachmentFileName(data, type, row, meta) {
                    return $('<span></span>').text(data).addClass(row.attachmentContentId)[0].outerHTML;
                }

                function renderAttachmentUsages(data, type, row, meta) {
                    if (type === 'display') {
                        var usagesHtml = '';
                        if (data.links.length === 0 && data.restrictedCount === 0) {
                            usagesHtml += 'n/a';
                        } else {
                            usagesHtml += '<a href="javascript:;" title="visible / restricted">' + data.links.length + ' / ' + data.restrictedCount + '</a>';
                        }
                        return usagesHtml;
                    } else if(type === 'type') {
                        return 'string';
                    } else {
                        return null;
                    }
                }
                <core_rt:if test="${not empty releaseId and writeAccessUser}">
                    let errorMsgClone = $("#errorMsgContainer").find("div:first").clone(true, true);
                    function evaluateCLIFiles() {
                       let releaseId = "${releaseId}";
                       $("#evaluateCLIFiles").attr("disabled", "disabled").text("<liferay-ui:message key="evaluating" />");
                        $.ajax({
                            url: '<%=evaluateCLIAttachments%>',
                            type: "GET",
                            success: function(result){
                               $("#evaluateCLIFiles").removeAttr("disabled").text("<liferay-ui:message key="evaluate.cli.files" />");
                               if(result.error) {
                                   $("#evaluateCLIFiles").removeClass("btn-primary").addClass("btn-danger");
                                   errorMsgClone.removeClass("alert-info d-none").addClass("alert-warning").find("span:first")
                                   .text("<liferay-ui:message key="an.error.happened.while.communicating.with.the.server" />")
                                   $("#errorMsgContainer").html("").append(errorMsgClone);
                                   return;
                               }

                               if(!Object.keys(result).length) {
                                   errorMsgClone.removeClass("alert-info d-none").addClass("alert-warning").find("span:first")
                                   .text("<liferay-ui:message key="no.super.set.cli.attachment.found" />");
                                   $("#errorMsgContainer").html("").append(errorMsgClone);
                                   return;
                               }
                               for( let [attachmentContentId, {superAttachmentContentId, superFilename}] of Object.entries(result)){
                                   let tr = $('.' + attachmentContentId).parents("tr:first")
                                   tr.find("td").each(function(){
                                       rowData = table.row(tr).data();
                                       rowData.superAttachmentId = superAttachmentContentId;
                                       rowData.superAttachmentFilename = superFilename;
                                       $(this).addClass('text-white').css("background-color", "#adaec1 !important");
                                   })
                               }
                               errorMsgClone.removeClass("alert-warning d-none").addClass("alert-info").find("span:first")
                               .text("<liferay-ui:message key="cli.attachment.evaluation.completed" />");
                               $("#errorMsgContainer").html("").append(errorMsgClone);
                            }});
                    }
                </core_rt:if>
            });
        </script>
    </core_rt:if>
</core_rt:if>
