<%--
  ~ Copyright Siemens AG, 2013-2017.
  ~ Copyright TOSHIBA CORPORATION, 2021.
  ~ Copyright Toshiba Software Development (Vietnam) Co., Ltd., 2021.
  ~ Part of the SW360 Portal Project.
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
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.Obligation" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.ObligationLevel" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.ObligationType" %>
<%@ page import="org.eclipse.sw360.datahandler.thrift.licenses.ObligationNode" %>

<jsp:useBean id="todo" class="org.eclipse.sw360.datahandler.thrift.licenses.Obligation" scope="request" />
<jsp:useBean id="obligList" type="java.util.List<org.eclipse.sw360.datahandler.thrift.licenses.Obligation>" scope="request"/>
<jsp:useBean id="obligationEdit" type="org.eclipse.sw360.datahandler.thrift.licenses.Obligation" scope="request"/>
<jsp:useBean id="obligationAction" class="java.lang.String" scope="request"/>

<portlet:actionURL var="addURL" name="addObligations">
</portlet:actionURL>

<div class="container">
	<div class="row">
		<div class="col">
            <div class="row portlet-toolbar">
				<div class="col-auto">
					<div class="btn-toolbar" role="toolbar">
                        <div class="btn-group">
                            <core_rt:if test="${obligationAction == 'edit'}">
                                <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="update.obligation" /></button>
                            </core_rt:if>
                            <core_rt:if test="${obligationAction != 'edit'}">
                                <button type="button" class="btn btn-primary" data-action="save"><liferay-ui:message key="create.obligation" /></button>
                            </core_rt:if>
                        </div>
                        <div class="btn-group">
                            <button type="button" class="btn btn-light" data-action="cancel"><liferay-ui:message key="cancel" /></button>
                        </div>
					</div>
				</div>
            </div>

            <div class="row">
                <div class="col">
                    <form id="todoAddForm" name="todoAddForm" action="<%=addURL%>" method="post" class="form needs-validation" novalidate>
                        <table id="todoAddTable" class="table edit-table three-columns">
                            <thead>
                                <tr>
                                    <th colspan="3"></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr>
                                    <td style="display: flex; width: 100%;">
                                        <div style="flex: 1; margin-right: 1rem;" class="form-group">
                                            <label for="todoTitle"><liferay-ui:message key="title" /></label>
                                            <input id="todoTitle" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.title" />" name="<portlet:namespace/><%=Obligation._Fields.TITLE%>"/>
                                            <div class="invalid-feedback" id="empty-title">
                                                <liferay-ui:message key="please.enter.a.title" />
                                            </div>
                                            <div class="invalid-feedback" id="duplicate-obl">
                                                <liferay-ui:message key="an.obligation.with.the.same.name.already.exists" />
                                            </div>
                                        </div>
                                        <div class="form-group" style="display: none;">
                                            <label for="obligsText"><liferay-ui:message key="text" /></label>
                                            <input id="obligsText" type="text" required class="form-control" placeholder="<liferay-ui:message key="enter.text" />" name="<portlet:namespace/><%=Obligation._Fields.TEXT%>"/>
                                            <div class="invalid-feedback">
                                                <liferay-ui:message key="please.enter.a.text" />
                                            </div>
                                        </div>
                                        <div style="flex: 1; margin-right: 1rem;" class="form-group">
                                            <label for="obligationType"><liferay-ui:message key="obligation.type" /></label>
                                            <select class="form-control" id="obligationType" name="<portlet:namespace/><%=Obligation._Fields.OBLIGATION_TYPE%>">
                                                <option value="">Select Obligation Type</option>
                                                <sw360:DisplayEnumOptions type="<%=ObligationType.class%>" selected="${todo.obligationType}"/>
                                            </select>
                                        </div>
                                        <div style="flex: 1; margin-right: 1rem;" class="form-group">
                                            <label for="obligationLevel"><liferay-ui:message key="obligation.level" /></label>
                                            <select class="form-control" id="obligationLevel" name="<portlet:namespace/><%=Obligation._Fields.OBLIGATION_LEVEL%>">
                                                <sw360:DisplayEnumOptions type="<%=ObligationLevel.class%>" selected="${todo.obligationLevel}"/>
                                            </select>
                                            <small class="form-text">
                                                <sw360:DisplayEnumInfo type="<%=ObligationLevel.class%>"/>
                                                <liferay-ui:message key="learn.more.about.obligation.level"/>
                                            </small>
                                        </div>
                                    </td>
                                </tr>
                                <%@ include file="obligationTextTree.jsp" %>
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
<script>
    // -------------- This is for Edit / Clone function -----------------
    // Keywords from OSADL obligation text
    var keywords = {
        "Obligation": ["YOU MUST", "YOU MUST NOT"],
        "Other": ["USE CASE",
                  "IF",
                  "EITHER",
                  "EITHER IF",
                  "OR",
                  "OR IF",
                  "EXCEPT IF",
                  "EXCEPT IF NOT",
                  "ATTRIBUTE",
                  "ATTRIBUTE NOT",
                  "COMPATIBILITY",
                  "DEPENDING COMPATIBILITY",
                  "INCOMPATIBILITY",
                  "PATENT HINTS",
                  "COPYLEFT CLAUSE",
                  ]
    };


    // Sort array based on item length
    function sortArray(arr, type) {
        if (type == 'ASC') {
            return arr.sort(function(a,b) {
                return a.length - b.length;
            })
        } else {
            return arr.sort(function(a,b) {
                return b.length - a.length;
            })
        }
    }

    // Assume that each action is a single word
    // Because there are many actions seperated by OR, need to find the positions of OR and detect all actions
    function getActions(text) {
        if (text.includes('OR')) {      // If there is OR
            const words = text.split(' ');

            // Because there is the case Object also containts OR, need to check the position of OR
            let ORpos = [];

            for (let i = 0; i < words.length; i++) {
                if (words[i] == 'OR') {
                    ORpos.push(i);
                }
            }

            // The first OR position is not 1, it means that the format is not 'verbA or verbB'
            if (ORpos[0] != 1) {
                return text.split(' ')[0];  // The first word is the verb
            } else {    // There are many ORs, check if they are equidistant
                let lastORPos = ORpos[0];

                for (let i = 0; i < ORpos.length - 1; i++) {
                    if (ORpos[i + 1] == ORpos[i] + 2) {
                        lastORPos = ORpos[i + 1];
                    } else {
                        break;
                    }
                }

                let result = [];

                for (let i = 0; i < lastORPos + 1; i++) {
                    result.push(words[i]);
                }

                if (lastORPos < words.length - 1) {
                    result.push(words[lastORPos + 1]);
                }

                return result.join(' ');
            }
        } else {                        // If ther is no OR, the first word is the verb
            return text.split(' ')[0];
        }
    }

    function parse(text) {
        text = text.trim();

        let allKeywords = sortArray(keywords['Obligation'].concat(keywords['Other']));

        for (let i = 0; i < allKeywords.length; i++) {
            if (text.startsWith(allKeywords[i])) {  // Sentence starts with keyword, this is normat format
                if (keywords['Obligation'].includes(allKeywords[i])) {  // This is an obligation statement
                    const languageElement = allKeywords[i];

                    let actionAndObjectText = text.substr(languageElement.length).trim();

                    const actions = getActions(actionAndObjectText);

                    const object = actionAndObjectText.substr(actions.length).trim();

                    return ['<Obligation>', languageElement, actions, object];
                } else {    // This is Other cases
                    const type = allKeywords[i];
                    const value = text.substr(type.length).trim();
                    return [type, value];
                }
            }
        }

        // Sentence does not start with keyword
        // The first uppercased word is the Type
        let type = '';
        let value = '';

        let words = text.split(' ');

        for (let i = 0; i < words.length; i++) {
            if (words[i] === words[i].toUpperCase() ) {
                type = type + ' ' + words[i];
            } else {
                break;
            }
        }

        type = type.trim();

        // If sentence does not start with an uppercases word, the first word is the Type
        if (type.length == 0) {
            type = words[0];
        }

        // The remained is value
        value = text.substr(type.length);

        return [type, value];
    }

    var lines = [];

    function getLevel(line) {
        let level = 0;

        for (let i = 0; i < line.length; i++) {
            if (line[i] == '\t') {
                level += 1;
            } else {
                return level;
            }
        }

        return level;
    }

    function getParentLineId(lineId) {
        let currentLevel = lines[lineId].level;

        for (let i = lineId; i >= 0; i--) {
            if (lines[i].level == currentLevel - 1) {
                return i;
            }
        }

        return -1;
    }

    function setLineLevel(lines) {
        let refinedLines = [];

        for (let i = 0; i < lines.length; i++) {
            if (lines[i].length == 0) {
                continue;
            }

            let currentLevel = getLevel(lines[i]);

            let lineWithLevel = { 'id': i, 'text': lines[i].replace(/^\t+/g,""), 'level': currentLevel, 'path': '-1'};

            refinedLines.push(lineWithLevel);
        }

        return refinedLines;
    }

    function setLinePath(lines) {
        let refinedLines = [];

        for (let i = 0; i < lines.length; i++) {
            let currentLine = lines[i];

            let parentLinePath = '-1';

            let parentLineId = getParentLineId(currentLine.id);

            if (parentLineId >= 0) {
                parentLinePath = lines[parentLineId].path;
            }

            currentLine.path = (parentLinePath + ' ' + currentLine.id).trim();

            refinedLines.push(currentLine);
        }

        return refinedLines;
    }

    function buildTreeObject() {
        let rootNode = {'val': ['ROOT'], 'children': [], 'path': '-1'};

        return addNode(rootNode);
    }

    function addNode(node) {
        for (let i = 0; i < lines.length; i++) {
            if (lines[i].path.substr(0, lines[i].path.lastIndexOf(' ')) == node.path) {
                let childNode = {'val': parse(lines[i].text), 'children': [], 'path': lines[i].path};
                node.children.push(childNode);
            }
        }

        for (let i = 0; i < node.children.length; i++) {
            addNode(node.children[i]);
        }

        return node;
    }

    function buildTreeNodeFromText(text) {
        lines = text.split('\n');

        lines = setLineLevel(lines);

        lines = setLinePath(lines);

        let tree = buildTreeObject();

        buildNode(tree, '#root');
    }

    function buildNode(node, liTag) {
        switch (node.val.length) {
            case 1: $(liTag).find('input').first().val(node.val[0]);
                break;
            case 2: $(liTag).find('.elementType').first().val(node.val[0]);
                $(liTag).find('.other').first().val(node.val[1]);
                break;
            case 4: $(liTag).find('.elementType').first().val(node.val[0]);
                $(liTag).find('.elementType').first().change();
                $(liTag).find('.obLangElement').first().val(node.val[1]);
                $(liTag).find('.obAction').first().val(node.val[2]);
                $(liTag).find('.obObject').first().val(node.val[3]);
                break;
            default:
                break;
        }

        // Add child nodes
        for (let i = 0; i < node.children.length; i++) {
            $(liTag).find('[data-func=add-child]').first().click();

            buildNode(node.children[i], $(liTag).find('li').last());
        }
    }

    require(['jquery', 'modules/dialog', 'modules/validation' ], function($, dialog, validation) {
        var action = '${obligationAction}';

        let obligationObj = jQuery.parseJSON(JSON.stringify(${ obligationJson }));
        let obligationListObj = jQuery.parseJSON(JSON.stringify(${ obligationListJson }));

        if (action == 'edit') {
            $('[data-action="save"]').text("Update Obligation");
        }

        $(function () {

            if (action != '') {
                var oblType = obligationObj.obligationType;

                switch (oblType) {
                    case "PERMISSION":
                        $('#obligationType').val("0");
                        break
                    case "RISK":
                        $('#obligationType').val("1");
                        break
                    case "EXCEPTION":
                        $('#obligationType').val("2");
                        break
                    case "RESTRICTION":
                        $('#obligationType').val("3");
                        break
                    case "OBLIGATION":
                        $('#obligationType').val("4");
                        break
                    default:
                        $('#obligationType').val($("#obligationType option:first").val());
                }

                var oblLevel = obligationObj.obligationLevel;

                switch (oblLevel) {
                    case "ORGANISATION_OBLIGATION":
                        $('#obligationLevel').val("0");
                        break;
                    case "PROJECT_OBLIGATION":
                        $('#obligationLevel').val("1");
                        break;
                    case "COMPONENT_OBLIGATION":
                        $('#obligationLevel').val("2");
                        break;
                    case "LICENSE_OBLIGATION":
                        $('#obligationLevel').val("3");
                        break;
                    default:
                        $('#obligationLevel').val("0");
                }
            }

            $('.invalid-feedback').css('display', 'none');
            $('.invalid-feedback').removeClass('d-block');
            validation.enableForm('#todoAddForm');

            $('.portlet-toolbar button[data-action="cancel"]').on('click', function() {
                var baseUrl = '<%= PortletURLFactoryUtil.create(request, portletDisplay.getId(), themeDisplay.getPlid(), PortletRequest.RENDER_PHASE) %>';
                var portletURL = Liferay.PortletURL.createURL( baseUrl )
                    .setParameter('<%=PortalConstants.PAGENAME%>','<%=PortalConstants.PAGENAME_VIEW%>')
                window.location = portletURL.toString();
            });

            $('.portlet-toolbar button[data-action="save"]').on('click', function() {
                $('.invalid-feedback').css('display', 'none');

                $('.invalid-feedback').removeClass('d-block');

                if (checkObligation()) {
                    const tree = readNode('#root');

                    const jsonTextTree = JSON.stringify(tree);

                    document.getElementById("obligsText").value = jsonTextTree;

                    $('#todoAddForm').submit();
                }
            });

            function readNode(currentNode) {
                var nodeData = {val:[], children:[]};

                nodeData.val = getNodeValues(currentNode);

                const childNodes = $(currentNode).children('ul');

                $(childNodes).each(function(key, childNode) {
                    var tmp = $(childNode).children('.tree-node').first();
                    nodeData.children.push(readNode(tmp));
                });

                return nodeData;
            }

            function getNodeValues(node) {
                const children = $(node).children();

                var nodeValues = [];

                $.each(children, function(key, child) {
                    if ($(child).is('input') && $(child).css('display') != 'none') {
                        nodeValues.push($(child).val().trim());
                    }
                });

                if ($(node).find('.elementType').val() == '<Obligation>') {
                    nodeValues.push("UNDEFINED");
                }

                if (nodeValues.length > 0 && nodeValues[0] == '<Obligation>') {
                    nodeValues[0] = 'Obligation';
                }

                return nodeValues;
            }

            function checkObligation() {
                let errorList = [];
                let title = $("#todoTitle").val();

                if (title.trim().length == 0) {
                    errorList.push('empty-title');
                }

                for (let i = 0; i < obligationListObj.length; i++) {
                    let obligationTitle = obligationListObj[i].title;

                    if (obligationTitle == title.trim() &&
                        (obligationTitle != obligationObj.title || action != 'edit')) {
                        errorList.push('duplicate-obl');
                        break;
                    }
                }

                var obligationText = $('#out').text().substring(title.length).replaceAll(" ","").replaceAll("\n","");

                if (obligationText == '') {
                    errorList.push('empty-text');
                }

                if (errorList.length === 0) {
                    return true;
                } else {
                    showError(errorList);
                    return false;
                }
            }

            function showError(errorList) {
                errorList.forEach(e => {
                    $('#' + e).addClass('d-block');
                });
            }
        });
    });
</script>
