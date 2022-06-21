/*
 * Copyright toshiba, 2022. Part of the SW360 Portal Project.
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.exporter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.sw360.datahandler.common.SW360Utils;
import org.eclipse.sw360.datahandler.thrift.SW360Exception;
import org.eclipse.sw360.datahandler.thrift.ThriftUtils;
import org.eclipse.sw360.datahandler.thrift.components.ComponentService;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.components.ReleaseLinkJSON;
import org.eclipse.sw360.datahandler.thrift.projects.Project;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectService;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.exporter.helper.ExporterHelper;
import org.eclipse.sw360.exporter.helper.ProjectHelper;
import org.eclipse.sw360.exporter.helper.ReleaseHelper;
import org.eclipse.sw360.exporter.utils.ReleaseUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.thrift.projects.Project._Fields.*;

public class ProjectExporterNetWork extends ExcelExporter<Project, ProjectHelper> {

    private static final Map<String, String> nameToDisplayName;

    private static final Logger log = LogManager.getLogger(ProjectExporterNetWork.class);

    static {
        nameToDisplayName = new HashMap<>();
        nameToDisplayName.put(ID.getFieldName(), "project ID");
        nameToDisplayName.put(NAME.getFieldName(), "project name");
        nameToDisplayName.put(STATE.getFieldName(), "project state");
        nameToDisplayName.put(CREATED_BY.getFieldName(), "created by");
        nameToDisplayName.put(CREATED_ON.getFieldName(), "creation date");
        nameToDisplayName.put(PROJECT_RESPONSIBLE.getFieldName(), "project responsible");
        nameToDisplayName.put(LEAD_ARCHITECT.getFieldName(), "project lead architect");
        nameToDisplayName.put(TAG.getFieldName(), "project tag");
        nameToDisplayName.put(BUSINESS_UNIT.getFieldName(), "group");
        nameToDisplayName.put(RELEASE_CLEARING_STATE_SUMMARY.getFieldName(), "release clearing state summary");
        nameToDisplayName.put(EXTERNAL_IDS.getFieldName(), "external IDs");
        nameToDisplayName.put(VISBILITY.getFieldName(), "visibility");
        nameToDisplayName.put(PROJECT_TYPE.getFieldName(), "project type");
        nameToDisplayName.put(LINKED_PROJECTS.getFieldName(), "linked projects with relationship");
        nameToDisplayName.put(RELEASE_ID_TO_USAGE.getFieldName(), "releases with usage");
        nameToDisplayName.put(CLEARING_TEAM.getFieldName(), "clearing team");
        nameToDisplayName.put(PREEVALUATION_DEADLINE.getFieldName(), "pre-evaluation deadline");
        nameToDisplayName.put(SYSTEM_TEST_START.getFieldName(), "system test start");
        nameToDisplayName.put(SYSTEM_TEST_END.getFieldName(), "system test end");
        nameToDisplayName.put(DELIVERY_START.getFieldName(), "delivery start");
        nameToDisplayName.put(PHASE_OUT_SINCE.getFieldName(), "phase out since");
        nameToDisplayName.put(PROJECT_OWNER.getFieldName(), "project owner");
        nameToDisplayName.put(OWNER_ACCOUNTING_UNIT.getFieldName(), "owner accounting unit");
        nameToDisplayName.put(OWNER_GROUP.getFieldName(), "owner group");
        nameToDisplayName.put(OWNER_COUNTRY.getFieldName(), "owner country");
        nameToDisplayName.put(VENDOR_ID.getFieldName(), "vendor id");
    }

    private static final List<Project._Fields> PROJECT_REQUIRED_FIELDS = ImmutableList.<Project._Fields>builder()
            .add(NAME)
            .add(VERSION)
            .add(BUSINESS_UNIT)
            .add(PROJECT_TYPE)
            .add(TAG)
            .add(CLEARING_STATE)
            .build();

    public static final List<Project._Fields> PROJECT_RENDERED_FIELDS = Project.metaDataMap.keySet()
            .stream()
            .filter(k -> PROJECT_REQUIRED_FIELDS.contains(k))
            .collect(Collectors.toList());

    public static List<String> HEADERS = PROJECT_RENDERED_FIELDS
            .stream()
            .map(Project._Fields::getFieldName)
            .map(n -> SW360Utils.displayNameFor(n, nameToDisplayName))
            .collect(Collectors.toList());

    public static List<String> HEADERS_EXTENDED_BY_RELEASES = ExporterHelper.addSubheadersWithPrefixesAsNeeded(HEADERS, ReleaseExporter.RELEASE_HEADERS_PROJECT_EXPORT, "release: ");

    public ProjectExporterNetWork(ComponentService.Iface componentClient, ProjectService.Iface projectClient, User user, List<Project> projects, boolean extendedByReleases) throws SW360Exception {
        super(new ProjectHelper(projectClient, user, extendedByReleases, new ReleaseHelper(componentClient, user)));
        preloadRelatedDataFor(projects, extendedByReleases, user);
    }

    public ProjectExporterNetWork(ComponentService.Iface componentClient, ProjectService.Iface projectClient, User user,
                                  boolean extendedByReleases) throws SW360Exception {
        super(new ProjectHelper(projectClient, user, extendedByReleases, new ReleaseHelper(componentClient, user)));
    }

    private void preloadRelatedDataFor(List<Project> projects, boolean withLinkedOfLinked, User user) throws SW360Exception {
        Function<Function<Project, Map<String, ?>>, Set<String>> extractIds = mapExtractor -> projects
                .stream()
                .map(mapExtractor)
                .filter(Objects::nonNull)
                .map(Map::keySet)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());

        Set<String> linkedProjectIds = extractIds.apply(Project::getLinkedProjects);
        Map<String, Project> projectsById = ThriftUtils.getIdMap(helper.getProjects(linkedProjectIds, user));
        helper.setPreloadedLinkedProjects(projectsById);

        // Set<String> linkedReleaseIds = extractIds.apply(Project::getReleaseIdToUsage);
        Set<String> linkedReleaseIds = new HashSet<>();
        for (Project project : projects) {
            try {
                String releaseNetwork = project.getReleaseRelationNetwork();
                ObjectMapper mapper = new ObjectMapper();
                List<ReleaseLinkJSON> listReleaseLinkJsonFatten = new ArrayList<>();
                List<ReleaseLinkJSON> listReleaseLinkJson = new ArrayList<>();
                if (!releaseNetwork.isEmpty()) {
                    List<ReleaseLinkJSON> list = mapper.readValue(releaseNetwork, new TypeReference<List<ReleaseLinkJSON>>() {
                    });
                    listReleaseLinkJson.addAll(list);
                }

                for (ReleaseLinkJSON release : listReleaseLinkJson) {
                    ReleaseUtils.flattenRelease(release, listReleaseLinkJsonFatten);
                }

                for (ReleaseLinkJSON release : listReleaseLinkJsonFatten) {
                    linkedReleaseIds.add(release.getReleaseId());
                }

            } catch (JsonProcessingException e) {
                log.error("release id to projects has error: " + e);
            }
        }
        preloadLinkedReleases(linkedReleaseIds, withLinkedOfLinked);
    }

    private void preloadLinkedReleases(Set<String> linkedReleaseIds, boolean withLinkedOfLinked) throws SW360Exception {
        Map<String, Release> releasesById = ThriftUtils.getIdMap(helper.getReleases(linkedReleaseIds));
        if (withLinkedOfLinked) {
            Set<String> linkedOfLinkedReleaseIds = releasesById
                    .values()
                    .stream()
                    .map(Release::getReleaseIdToRelationship)
                    .filter(Objects::nonNull)
                    .map(Map::keySet)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());

            Map<String, Release> joinedMap = new HashMap<>();
            Map<String, Release> linkedOfLinkedReleasesById = ThriftUtils.getIdMap(helper.getReleases(linkedOfLinkedReleaseIds));
            joinedMap.putAll(releasesById);
            joinedMap.putAll(linkedOfLinkedReleasesById);
            releasesById = joinedMap;
        }
        helper.setPreloadedLinkedReleases(releasesById, withLinkedOfLinked);
    }

}
