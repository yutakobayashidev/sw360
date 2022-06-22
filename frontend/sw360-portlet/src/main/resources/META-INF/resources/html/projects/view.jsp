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
<%@ page import="com.liferay.portal.kernel.portlet.PortletURLFactoryUtil" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.Project" %>
<%@ page import="org.eclipse.sw360.portal.common.PortalConstants" %>
<%@ page import="javax.portlet.PortletRequest" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ProjectType" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ProjectState" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.projects.ProjectClearingState" %>
<%@ page import="org.eclipse.sw360.portal.common.FossologyConnectionHelper" %>

<%@ include file="/html/init.jsp" %>
<%-- the following is needed by liferay to display error messages--%>
<%@ include file="/html/utils/includes/errorKeyToMessage.jspf"%>
<%@include file="/html/projects/includes/projects/clearingRequest.jspf" %>

<portlet:defineObjects/>
<liferay-theme:defineObjects/>

<jsp:useBean id="projectType" class="java.lang.String" scope="request"/>
<jsp:useBean id="project" class="org.eclipse.sw360.datahandler.thrift.projects.Project" scope="request"/>
<jsp:useBean id="projectResponsible" class="java.lang.String" scope="request"/>
<jsp:useBean id="releaseClearingStateSummary" class="org.eclipse.sw360.datahandler.thrift.components.ReleaseClearingStateSummary" scope="request"/>
<jsp:useBean id="businessUnit" class="java.lang.String" scope="request"/>
<jsp:useBean id="tag" class="java.lang.String" scope="request"/>
<jsp:useBean id="name" class="java.lang.String" scope="request"/>
<jsp:useBean id="state" class="java.lang.String" scope="request"/>

<core_rt:set var="stateAutoC" value='<%=PortalConstants.STATE%>'/>
<core_rt:set var="projectTypeAutoC" value='<%=PortalConstants.PROJECT_TYPE%>'/>
<core_rt:set var="FOSSOLOGY_CONNECTION_ENABLED" value="<%=FossologyConnectionHelper.getInstance().isFossologyConnectionEnabled()%>"/>
<core_rt:set var="pageName"  value="<%= request.getParameter("pagename") %>" />

<portlet:resourceURL var="exportProjectsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.EXPORT_TO_EXCEL%>"/>
</portlet:resourceURL>

<portlet:resourceURL var="deleteAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.REMOVE_PROJECT%>'/>
</portlet:resourceURL>

<portlet:renderURL var="impProjectURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_IMPORT%>"/>
</portlet:renderURL>

<portlet:renderURL var="addProjectURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.PAGENAME_EDIT%>"/>
</portlet:renderURL>

<portlet:renderURL var="friendlyProjectURL">
    <portlet:param name="<%=PortalConstants.PAGENAME%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>"/>
</portlet:renderURL>

<portlet:actionURL var="applyFiltersURL" name="applyFilters">
</portlet:actionURL>

<portlet:resourceURL var="loadClearingStateAjaxURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.GET_CLEARING_STATE_SUMMARY%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="loadProjectsURL">
    <portlet:param name="<%=PortalConstants.ACTION%>" value='<%=PortalConstants.LOAD_PROJECT_LIST%>'/>
</portlet:resourceURL>

<portlet:resourceURL var="generateExcelReport">
    <portlet:param name="<%=PortalConstants.ACTION%>" value="<%=PortalConstants.EMAIL_EXPORTED_EXCEL%>"/>
    <portlet:param name="<%=PortalConstants.PROJECT_ID%>" value="${docid}"/>
</portlet:resourceURL>

<div class="container" style="display: none;">
    <div class="row">
        <div class="col-3 sidebar">
            <div class="card-deck">
                <div id="searchInput" class="card">
                    <div class="card-header">
                        <liferay-ui:message key="advanced.search" />
                    </div>
                    <div class="card-body">
                    <form action="<%=applyFiltersURL%>" method="post">
                        <div class="form-group">
                            <label for="project_name"><liferay-ui:message key="project.name" /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.NAME%>"
                                           value="<sw360:out value="${name}"/>" id="project_name">
                            </div>
                            <div class="form-group">
                                <label for="project_version"><liferay-ui:message key="project.version" /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.VERSION%>"
                                       value="<sw360:out value="${version}"/>" id="project_version">
                            </div>
                            <div class="form-group">
                                <label for="project_type"><liferay-ui:message key="project.type" /></label>
                                <select class="form-control form-control-sm" id="project_type" name="<portlet:namespace/><%=Project._Fields.PROJECT_TYPE%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>"></option>
                                    <sw360:DisplayEnumOptions type="<%=ProjectType.class%>" selectedName="${projectType}" useStringValues="true"/>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="project_responsible"><liferay-ui:message key="project.responsible.email" /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.PROJECT_RESPONSIBLE%>"
                                       value="<sw360:out value="${projectResponsible}"/>" id="project_responsible">
                            </div>
                            <div class="form-group">
                                <label for="group"><liferay-ui:message key="group" /></label>
                                <select class="form-control form-control-sm" id="group" name="<portlet:namespace/><%=Project._Fields.BUSINESS_UNIT%>">
                                    <option value=""
                                            <core_rt:if test="${empty businessUnit}"> selected="selected"</core_rt:if>></option>
                                    <core_rt:forEach items="${organizations}" var="org">
                                        <option value="<sw360:out value="${org}"/>"
                                                <core_rt:if test="${org == businessUnit}"> selected="selected"</core_rt:if>
                                        ><sw360:out value="${org}"/></option>
                                    </core_rt:forEach>
                                </select>
                            </div>
                            <div class="form-group">
                               <label for="project_state"><liferay-ui:message key="state" /></label>
                                <select class="form-control form-control-sm" id="project_state" name="<portlet:namespace/><%=Project._Fields.STATE%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>"></option>
                                    <sw360:DisplayEnumOptions type="<%=ProjectState.class%>" selectedName="${state}" useStringValues="true"/>
                                </select>
                            </div>
                            <div class="form-group">
                               <label for="project_clearing_state"><liferay-ui:message key="clearing.state" /></label>
                                <select class="form-control form-control-sm" id="project_clearing_state" name="<portlet:namespace/><%=Project._Fields.CLEARING_STATE%>">
                                    <option value="<%=PortalConstants.NO_FILTER%>"></option>
                                    <sw360:DisplayEnumOptions type="<%=ProjectClearingState.class%>" selectedName="${clearingState}" useStringValues="true"/>
                                </select>
                            </div>
                            <div class="form-group">
                                <label for="tag"><liferay-ui:message key="tag" /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.TAG%>"
                                        value="<sw360:out value="${tag}"/>" id="tag">
                            </div>
                            <div class="form-group">
                                <label for="additional_data"><liferay-ui:message key="additional.data" /></label>
                                <input type="text" class="form-control form-control-sm" name="<portlet:namespace/><%=Project._Fields.ADDITIONAL_DATA%>"
                                        value="<sw360:out value="${additionalData}"/>" id="additional_data">
                            </div>
                            <button type="submit" class="btn btn-primary btn-sm btn-block"><liferay-ui:message key="search" /></button>
                        </form>
                    </div>
                </div>
            </div>
        </div>
        <div class="col">
            <div class="row portlet-toolbar">
                <div class="col-auto">
                    <div class="btn-toolbar" role="toolbar">
                        <div class="btn-group" role="group">
                            <button type="button" class="btn btn-primary" onclick="window.location.href='<%=addProjectURL%>'"><liferay-ui:message key="add.project" /></button>
                            <button type="button" class="btn btn-secondary" data-action="import-spdx-bom"><liferay-ui:message key="import.spdx.bom" /></button>
                        </div>
                        <div id="btnExportGroup" class="btn-group" role="group">
                            <button id="btnExport" type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                <liferay-ui:message key="export.spreadsheet" />
                                <clay:icon symbol="caret-bottom" />
                            </button>
                            <div class="dropdown-menu" aria-labelledby="btnExport">
                                <a class="dropdown-item" href="#" data-type="projectOnly"><liferay-ui:message key="projects.only" /></a>
                                <a class="dropdown-item" href="#" data-type="projectWithReleases"><liferay-ui:message key="projects.with.linked.releases" /></a>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="col portlet-title text-truncate" title="<liferay-ui:message key="projects" />">
                    <liferay-ui:message key="projects" />
                </div>
            </div>

            <div class="row">
                <div class="col">
                    <table id="projectsTable" class="table table-bordered"></table>
                </div>
            </div>

        </div>
    </div>
</div>
<%@ include file="/html/utils/includes/pageSpinner.jspf" %>

<div class="dialogs auto-dialogs">

    <div id="deleteProjectDialog" class="modal fade" tabindex="-1" role="dialog">
        <div class="modal-dialog modal-lg modal-dialog-centered modal-danger" role="document">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">
                        <clay:icon symbol="question-circle" />
                        <liferay-ui:message key="delete.project" />?
                    </h5>
                    <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                        <span aria-hidden="true">&times;</span>
                    </button>
                </div>
                <div class="modal-body">		 
					<p><liferay-ui:message key="do.you.really.want.to.delete.the.project.x" /></p>
                    <div data-hide="hasNoDependencies">
                        <p>
                        <liferay-ui:message key="this.project.x.contains" />
                        </p>
                        <ul>
                            <li data-hide="hasNoLinkedProjects"><span data-name="linkedProjects"></span> <liferay-ui:message key="linked.projects" /></li>
                            <li data-hide="hasNoLinkedReleases"><span data-name="linkedReleases"></span> <liferay-ui:message key="linked.releases" /></li>
                            <li data-hide="hasNoAttachments"><span data-name="attachments"></span> <liferay-ui:message key="attachments" /></li>
                        </ul>
                    </div>
                    <hr/>
                    <form>
                        <div class="form-group">
                            <label for="moderationDeleteCommentField"><liferay-ui:message key="please.comment.your.changes" /></label>
                            <textarea id="moderationDeleteCommentField" class="form-control" data-name="comment" rows="4" placeholder="<liferay-ui:message key="comment.your.request" />"></textarea>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-light" data-dismiss="modal"><liferay-ui:message key="cancel" /></button>
                    <button type="button" class="btn btn-danger"><liferay-ui:message key="delete.project" /></button>
                </div>
            </div>
        </div>
    </div>
</div>


<%@ include file="/html/utils/includes/requirejs.jspf" %>
<%@ include file="/html/utils/includes/importBom.jspf" %>
<script>
    AUI().use('liferay-portlet-url', function () {
        var PortletURL = Liferay.PortletURL;
        const clearingSummaryColumnIndex = 4;

        require(['jquery', 'modules/autocomplete', 'modules/dialog', 'modules/validation', 'bridges/datatables', 'utils/render', 'bridges/jquery-ui'], function($, autocomplete, dialog, validation, datatables, render) {
            var projectsTable;

             // initializing
            autocomplete.prepareForMultipleHits('state', ${stateAutoC});
            autocomplete.prepareForMultipleHits('project_type', ${projectTypeAutoC});
            projectsTable = createProjectsTable();

            // catch ctrl+p and print dataTable
            $(document).on('keydown', function(e){
                if(e.ctrlKey && e.which === 80){
                    e.preventDefault();
                    projectsTable.buttons('.custom-print-button').trigger();
                }
            });

             // register event handlers
            $('#searchInput input, #searchInput select, #searchInput textarea').on('input', function() {
                $('#btnExport').prop('disabled', true);
                <%--when filters are actually applied, page is refreshed and exportSpreadsheetButton enabled automatically--%>
            });
            $('#projectsTable').on('click', 'svg.delete', function(event) {
                var data = $(event.currentTarget).data();
                deleteProject(data.projectId, data.projectName, data.linkedProjectsCount, data.linkedReleasesCount, data.projectAttachmentCount);
            });
            $('#projectsTable').on('click', 'svg.disabledClearingRequest', function(event) {
                var disabledBtnData = $(event.currentTarget).data();

                if (disabledBtnData.accessDenied) {
                        dialog.warn(
                                '<liferay-ui:message key="you.do.not.have.write.access.to.the.project" />'
                            );
                } else {
                dialog.warn(
                        '<liferay-ui:message key="clearing.request.cannot.be.created.for.project.with.specific.bu.or.closed.or.private.project" />'
                    );
                }
            });

            $('#projectsTable').on('click', 'svg.clearing', function(event) {
                openSelectClearingDialog($(event.currentTarget).data('projectId'));
            });
            $('#btnExportGroup a.dropdown-item').on('click', function(event) {
                exportSpreadsheet($(event.currentTarget).data('type'));
            });

             // helper functions
            function makeProjectUrl(projectId, page) {
                var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>')
                    .setParameter('<%=PortalConstants.PAGENAME%>', page)
                    .setParameter('<%=PortalConstants.PROJECT_ID%>', projectId);
                return portletURL.toString();
            }

            function replaceFriendlyUrlParameter(projectId, page) {
                var portletURL = '<%=friendlyProjectURL%>'
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>', page)
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', projectId);
                return portletURL;
            }

            function replaceFriendlyUrlParameter(portletUrl, id, page) {
                return portletUrl
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_PAGENAME%>', page)
                    .replace('<%=PortalConstants.FRIENDLY_URL_PLACEHOLDER_ID%>', id);
            }

            // create and render datatable
            function createProjectsTable() {
                var projectsTable;

                projectsTable = datatables.create('#projectsTable', {
                    // the following two parameters must not be removed, otherwise it won't work anymore (probably due to datatable plugins)
                    bServerSide: true,
                    sAjaxSource: '<%=loadProjectsURL%>',

                    columns: [
                        {title: "<liferay-ui:message key="project.name" />", data: "name", render: {display: renderProjectNameLink}},
                        {title: "<liferay-ui:message key="description" />", data: "desc", render: {display: renderDescription}},
                        {title: "<liferay-ui:message key="project.responsible" />", data: "resp", render: {display: renderProjectResponsible}},
                        {title: "<liferay-ui:message key="state" />", data: "state", render: {display: renderStateBoxes} },
                        {title: "<span title=\"<liferay-ui:message key="release.clearing.state" />\"><liferay-ui:message key="license.clearing" /></span>", data: "clearing" },
                        {title: "<liferay-ui:message key="actions" />", data: "id", render: {display: renderProjectActions}, className: "four actions" }
                    ],
                    fnDrawCallback: function (oSettings) {
                        loadClearingStateSummaries();
                    },
                    language: {
                        url: "<liferay-ui:message key="datatables.lang" />",
                        loadingRecords: "<liferay-ui:message key="loading" />"
                    },
                    initComplete: datatables.showPageContainer
                }, [0, 1, 2, 3, 4], 5);

                return projectsTable;
            }

            function renderProjectActions(id, type, row) {
                var $actions = $('<div>', {
                        'class': 'actions'
                    }),
                    $editAction,
                    $clearingRequestAction,
                    $copyAction = render.linkTo(
                        makeProjectUrl(id, '<%=PortalConstants.PAGENAME_DUPLICATE%>'),
                        "",
                        '<svg class="lexicon-icon"><title><liferay-ui:message key="duplicate" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#paste"/></svg>'
                    ),
                    $deleteAction = $('<svg>', {
                        'class': 'delete lexicon-icon',
                        'data-project-id': id,
                        'data-project-name': row.name,
                        'data-linked-projects-count': row.lProjSize,
                        'data-linked-releases-count': row.lRelsSize,
                        'data-project-attachment-count': row.attsSize,
                    });

                $deleteAction.append($('<title>Delete</title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#trash"/>'));

                if (row.crId) {
                    $clearingRequestAction = $('<svg>', {
                        'class': 'viewClearingRequest lexicon-icon text-warning', // pending or in-progress clearing request
                        'data-clearing-request-id': row.crId,
                        'data-project-name': row.name,
                    }).append('<title><liferay-ui:message key="view.clearing.request" /></title>');
                } else if (row.isCrDisabledForProjectBU || row.cState === 'CLOSED' || row.visbility === 'PRIVATE') {
                    $clearingRequestAction = $('<svg>', {
                        'class': 'disabledClearingRequest lexicon-icon disabled',
                    }).append('<title><liferay-ui:message key="c.r.is.disabled.for.project.with.specific.bu.or.closed.or.private.project" /></title>');
                } else {
                    $clearingRequestAction = $('<svg>', {
                        'class': 'createClearingRequest lexicon-icon',
                        'data-project-id': id,
                        'data-project-name': row.name,
                        'data-linked-projects-count': row.lProjSize,
                        'data-linked-releases-count': row.lRelsSize,
                    }).append('<title><liferay-ui:message key="create.clearing.request" /></title>');
                }

                $clearingRequestAction.append($('<use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#check-square"/>'));

                if(row.cState == 'CLOSED' && ${isUserAdmin != 'Yes'} & !(row.isProjectMember)) {
                    $editAction = $('<svg class="lexicon-icon disabled"><title><liferay-ui:message key="only.administrators.can.edit.a.closed.project" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>');
                } else {
                    $editAction = render.linkTo(
                        makeProjectUrl(id, '<%=PortalConstants.PAGENAME_EDIT%>'),
                        "",
                        '<svg class="lexicon-icon"><title><liferay-ui:message key="edit" /></title><use href="/o/org.eclipse.sw360.liferay-theme/images/clay/icons.svg#pencil"/></svg>'
                    );
                }

                $actions.append($editAction, $clearingRequestAction, $copyAction, $deleteAction);
                return $actions[0].outerHTML;
            }

            function renderProjectNameLink(name, type, row) {
                return render.linkTo(replaceFriendlyUrlParameter('<%=friendlyProjectURL%>', row.id, '<%=PortalConstants.PAGENAME_DETAIL%>'), name);
            }

            function renderStateBoxes(state, type, row) {
                var projectStateBackgroundColour = getProjectStateBackgroundColour(state);
                var clearingStateBackgroundColour = getClearingStateBackgroundColour(row);

                var $state = $('<div>', {
                    'class': 'content-center'
                });
                var $psBox = $('<div>', {
                    'class': 'stateBox capsuleLeft ' + projectStateBackgroundColour
                }).text('PS').attr("title", "Project state: "+ row.state);
                var $csBox = $('<div>', {
                    'class': 'stateBox capsuleRight ' + clearingStateBackgroundColour
                }).text('CS').attr("title", "Project clearing state: " + row.cState);

                $state.append($psBox, $csBox);
                return $state[0].outerHTML;
            }

            function getProjectStateBackgroundColour(state) {
                if (state != null && state === 'ACTIVE') { // -> green
                    return '<%=PortalConstants.PROJECT_STATE_ACTIVE__CSS%>';
                } else {
                    return '<%=PortalConstants.PROJECT_STATE_INACTIVE__CSS%>';
                }
            }

            function getClearingStateBackgroundColour(row) {
                if (row != null && row.cState != null) {
                    switch (row.cState) {
                        case 'CLOSED': // -> green
                            return '<%=PortalConstants.CLEARING_STATE_CLOSED__CSS%>';
                        case 'IN_PROGRESS': // -> yellow
                            return '<%=PortalConstants.CLEARING_STATE_INPROGRESS__CSS%>';
                        case 'OPEN': // -> red
                            return '<%=PortalConstants.CLEARING_STATE_OPEN__CSS%>';
                    }
                }
                return '<%=PortalConstants.CLEARING_STATE_UNKNOWN__CSS%>';
            }

            function renderDescription(description, type, row) {
                if (description) {
                    return $("<span></span>").text(render.truncate(description, 140))[0].outerHTML;
                } else {
                    return "";
                }
            }

            function renderProjectResponsible(responsible, type, row) {
                if (responsible) {
                    return render.userEmail(responsible);
                } else {
                    return "";
                }
            }

            function loadClearingStateSummaries() {
                var tableData = projectsTable.data(),
                    queryPageSize = 10,
                    numberOfQueryPages = Math.ceil(tableData.length / queryPageSize),
                    queryPagesPageIds = [],
                    pageIds,
                    cell,
                    i, j, idx;

                for (i = 0; i < numberOfQueryPages; i++) {
                    pageIds = [];
                    for (j = 0; j < queryPageSize; j++) {
                        idx = (i * queryPageSize) + j;
                        /* make sure last page is handled correctly as it might not be a full page */
                        if (tableData[idx]) {
                            pageIds.push(tableData[idx].id);
                            cell = projectsTable.cell(idx, clearingSummaryColumnIndex);
                            cell.data('<liferay-ui:message key="loading" />');
                        } else {
                            break;
                        }
                    }
                    queryPagesPageIds.push(pageIds);
                }
                postClearingStateSummaryRequest(queryPagesPageIds);
            }

            function postClearingStateSummaryRequest(idSets) {
                if (idSets.length < 1) {
                    return;
                }

                var ids = idSets.shift();
                $.ajax({
                    type: 'POST',
                    url: '<%=loadClearingStateAjaxURL%>',
                    cache: false,
                    data: {
                        "<portlet:namespace/><%=Project._Fields.ID%>": ids
                    },
                    success: function (response) {
                        for (var i = 0; i < response.length; i++) {
                            var cell_clearingsummary = projectsTable.cell("#" + response[i].id, clearingSummaryColumnIndex),
                                isCreateCR = $('#' + response[i].id + ' td:eq(5)').find('.createClearingRequest');
                            cell_clearingsummary.data(displayClearingStateSummary(response[i].clearing));
                            if (!response[i].writeAccessUser && isCreateCR.length) {
                                isCreateCR.removeClass('createClearingRequest').addClass('disabledClearingRequest').attr('data-access-denied', true);
                            }
                        }
                    },
                    error: function () {
                        for (var i = 0; i < ids.length; i++) {
                            var cell = projectsTable.cell("#" + ids[i], clearingSummaryColumnIndex);
                            cell.data("Failed to load");
                        }
                    },
                    complete: function(xhr, status) {
                        /* even though former requests might have failed, we want to start all further ones */
                        postClearingStateSummaryRequest(idSets);
                    }
                });
            }

            function displayClearingStateSummary(clearing){
                function d(v) { return v == undefined ? 0 : v; }

                var resultElementAsString = "<span class=\"clearingstate\" title=\"Necessary data not found on server!\">Not available</span>",
                    releaseCount,
                    approvedCount;

                if (clearing) {
                    releaseCount = d(clearing.newRelease) + d(clearing.underClearing) + d(clearing.sentToClearingTool) + d(clearing.reportAvailable) + d(clearing.approved) + d(clearing.scanAvailable);
                    approvedCount = d(clearing.approved);
                    resultElementAsString = "<span class=\"clearingstate content-center\" title=\"" + approvedCount + (approvedCount === 1 ? " <liferay-ui:message key='release' />" : " <liferay-ui:message key='releases' />")
                        + " <liferay-ui:message key='out.of' /> " + releaseCount + (approvedCount === 1 ? " <liferay-ui:message key='has' />" : " <liferay-ui:message key='have' />")
                        + " <liferay-ui:message key='approved.clearing.reports.including.sub.project' />.\">" + approvedCount + "/" + releaseCount + "</span>";
                }

                return resultElementAsString;
            }

            // Export Spreadsheet action
            function exportSpreadsheet(type) {
                var  isEMailEnabled = <%=PortalConstants.SEND_PROJECT_SPREADSHEET_EXPORT_TO_MAIL_ENABLED%>;
                if(isEMailEnabled){
                    var isWithReleases = (type === 'projectWithReleases' ? 'true' : 'false');
                    $.ajax({
                        type: 'POST',
                        url: '<%=generateExcelReport%>',
                        cache: false,
                        data: {
                            "<portlet:namespace/><%=PortalConstants.EXTENDED_EXCEL_EXPORT%>": isWithReleases,
                        }
                    });
                } else {
                    var portletURL = PortletURL.createURL('<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RESOURCE_PHASE) %>')
                    .setParameter('<%=PortalConstants.ACTION%>', '<%=PortalConstants.EXPORT_TO_EXCEL_PROJECT_NETWORK%>');
                    portletURL.setParameter('<%=Project._Fields.NAME%>', $('#project_name').val());
                    portletURL.setParameter('<%=Project._Fields.TYPE%>', $('#project_type').val());
                    portletURL.setParameter('<%=Project._Fields.PROJECT_RESPONSIBLE%>', $('#project_responsible').val());
                    portletURL.setParameter('<%=Project._Fields.BUSINESS_UNIT%>', $('#group').val());
                    portletURL.setParameter('<%=Project._Fields.STATE%>', $('#project_state').val());
                    portletURL.setParameter('<%=Project._Fields.TAG%>', $('#tag').val());
                    portletURL.setParameter('<%=PortalConstants.EXTENDED_EXCEL_EXPORT%>', type === 'projectWithReleases' ? 'true' : 'false');
                    window.location.href = portletURL.toString();
                }
            }

            // delete action
            function deleteProject(projectId, name, linkedProjectsSize, linkedReleasesSize, attachmentsSize) {
                var $dialog;

                function deleteProjectInternal(callback) {
                    jQuery.ajax({
                        type: 'POST',
                        url: '<%=deleteAjaxURL%>',
                        cache: false,
                        data: {
                            "<portlet:namespace/><%=PortalConstants.PROJECT_ID%>": projectId,
                            "<portlet:namespace/><%=PortalConstants.MODERATION_REQUEST_COMMENT%>": btoa($("#moderationDeleteCommentField").val())
                        },
                        success: function (data) {
                            callback();

                            if (data.result == 'SUCCESS') {
                                projectsTable.row('#' + projectId).remove().draw(false);
                                $dialog.close();
                            }
                            else if (data.result == 'SENT_TO_MODERATOR') {
                                $dialog.info('<liferay-ui:message key="you.may.not.delete.the.project.but.a.request.was.sent.to.a.moderator" />', true);
                            } else if (data.result == 'IN_USE') {
                                $dialog.warning('<liferay-ui:message key="the.project.cannot.be.deleted.since.it.is.used.by.another.project" />');
                            }
                            else {
                                $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.project" />');
                            }
                        },
                        error: function () {
                            callback();
                            $dialog.alert('<liferay-ui:message key="i.could.not.delete.the.project" />');
                        }
                    });

                }

                $dialog = dialog.open('#deleteProjectDialog', {
                    name: name,
                    linkedProjects: linkedProjectsSize,
                    linkedReleases: linkedReleasesSize,
                    attachments: attachmentsSize,
                    hasNoDependencies: linkedProjectsSize == 0 && linkedReleasesSize == 0 && attachmentsSize == 0,
                    hasNoLinkedProjects: linkedProjectsSize == 0,
                    hasNoLinkedReleases: linkedReleasesSize == 0,
                    hasNoAttachments: attachmentsSize == 0
                }, function(submit, callback) {
                    deleteProjectInternal(callback);
                });
            }
        });
    });
</script>
