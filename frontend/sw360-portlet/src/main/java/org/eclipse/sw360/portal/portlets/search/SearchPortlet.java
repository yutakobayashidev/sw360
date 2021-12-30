/*
 * Copyright Siemens AG, 2013-2015, 2019. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.portal.portlets.search;

import org.eclipse.sw360.datahandler.thrift.search.SearchResult;
import org.eclipse.sw360.datahandler.thrift.search.SearchService;
import org.eclipse.sw360.datahandler.thrift.users.UserService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.portal.portlets.Sw360Portlet;
import org.eclipse.sw360.portal.users.UserCacheHolder;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.thrift.TException;
import org.osgi.service.component.annotations.ConfigurationPolicy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.portlet.*;

import com.google.common.base.Strings;
import com.liferay.portal.kernel.util.PortalUtil;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.eclipse.sw360.portal.common.PortalConstants.*;

@org.osgi.service.component.annotations.Component(
    immediate = true,
    properties = {
        "/org/eclipse/sw360/portal/portlets/base.properties",
        "/org/eclipse/sw360/portal/portlets/default.properties"
    },
    property = {
        "javax.portlet.name=" + SEARCH_PORTLET_NAME,

        "javax.portlet.display-name=Search Results",
        "javax.portlet.info.short-title=Search Results",
        "javax.portlet.info.title=Search Results",
        "javax.portlet.resource-bundle=content.Language",
        "javax.portlet.init-param.view-template=/html/search/view.jsp",
    },
    service = Portlet.class,
    configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class SearchPortlet extends Sw360Portlet {

    private static final Logger log = LogManager.getLogger(SearchPortlet.class);

    @Override
    public void doView(RenderRequest request, RenderResponse response) throws IOException, PortletException {
        final User user = UserCacheHolder.getUserFromRequest(request);
        String searchtext = request.getParameter(KEY_SEARCH_TEXT);
        String[] typeMaskArray = request.getParameterValues(TYPE_MASK);
        String searchQuery = PortalUtil.getOriginalServletRequest(PortalUtil.getHttpServletRequest(request)).getParameter("q");

        List<String> typeMask;
        if (typeMaskArray != null) { // premature optimization would add && typeMaskArray.length<6
            typeMask = Arrays.asList(typeMaskArray);
        } else {
            typeMask = Collections.emptyList();
            log.info("typeMask set to emptyList");
        }

        if (isNullOrEmpty(searchtext)) {
            searchtext = searchQuery;
        }
        searchtext = Strings.nullToEmpty(searchtext);

        List<SearchResult> searchResults = new ArrayList<>();

        if (!(typeMask.contains(TYPE_DEPARTMENT) && typeMask.size() == 1)) {
            List<String> searchType = typeMask.stream()
                                            .filter(type -> !type.equals(TYPE_DEPARTMENT))
                                            .collect(Collectors.toList());
            try {
                SearchService.Iface client = thriftClients.makeSearchClient();
                searchResults = client.searchFiltered(searchtext, user, searchType);
            } catch (TException e) {
                log.error("Search could not be performed!", e);
                searchResults = Collections.emptyList();
            }
        }

        if (typeMask.isEmpty() || typeMask.contains(TYPE_DEPARTMENT)) {
            searchDepartment(searchResults, searchtext);
        }

        // Set the results
        request.setAttribute(KEY_SEARCH_TEXT, searchtext);
        request.setAttribute(KEY_SUMMARY, searchResults);
        request.setAttribute(TYPE_MASK, typeMask);

        // Proceed with page rendering
        super.doView(request, response);
    }

    private void searchDepartment(List<SearchResult> searchResults, String searchtext) {
        List<SearchResult> departmentResults = new ArrayList<>();
        List<String> listSearch = new ArrayList<>();

        if (isNullOrEmpty(searchtext)) {
            return;
        } else {
            String[] searchs = searchtext.split(" ");
            for (String search : Arrays.asList(searchs)) {
                if (!isNullOrEmpty(search)) {
                    listSearch.add(search.toUpperCase());
                }
            }
        }

        try {
            UserService.Iface client = thriftClients.makeUserClient();
            Set<String> departments = client.getUserDepartments();
            if (departments != null) {
                for (String department : departments) {
                    SearchResult result = new SearchResult();
                    result.setName(department);
                    result.setType(TYPE_DEPARTMENT);
                    departmentResults.add(result);
                }
            }
        } catch (TException e) {
            log.error("Search could not be performed Departments!", e);
        }

        if (isNullOrEmpty(searchtext.trim())) {
            searchResults.addAll(departmentResults);
        } else {
            for (SearchResult departmentResult : departmentResults) {
                if (listSearch.stream().anyMatch(departmentResult.getName().toUpperCase()::contains)) {
                    searchResults.add(departmentResult);
                }
            }
        }
    }

}
