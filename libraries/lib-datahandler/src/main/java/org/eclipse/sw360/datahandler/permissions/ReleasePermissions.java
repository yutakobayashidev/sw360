/*
 * Copyright Siemens AG, 2014-2017. Part of the SW360 Portal Project.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.sw360.datahandler.permissions;

import com.google.common.collect.Sets;

import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.components.Release;
import org.eclipse.sw360.datahandler.thrift.users.RequestedAction;
import org.eclipse.sw360.datahandler.thrift.users.User;
import org.eclipse.sw360.datahandler.thrift.users.UserGroup;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.eclipse.sw360.datahandler.common.CommonUtils.nullToEmptySet;
import static org.eclipse.sw360.datahandler.common.CommonUtils.toSingletonSet;

/**
 * Created by bodet on 16/02/15.
 *
 * @author cedric.bodet@tngtech.com
 * @author alex.borodin@evosoft.com
 */
public class ReleasePermissions extends DocumentPermissions<Release> {

    private final Set<String> moderators;
    private final Set<String> contributors;
    private final Set<String> attachmentContentIds;

    protected ReleasePermissions(Release document, User user) {
        super(document, user);

        moderators = Sets.union(toSingletonSet(document.createdBy), nullToEmptySet(document.moderators));
        contributors = Sets.union(moderators, nullToEmptySet(document.contributors));
        attachmentContentIds = nullToEmptySet(document.getAttachments()).stream()
                .map(a -> a.getAttachmentContentId())
                .collect(Collectors.toSet());

    }

    @NotNull
    public static Predicate<Release> isVisible(final User user) {
        return input -> {
            return true;
        };
    }

    @Override
    public void fillPermissions(Release other, Map<RequestedAction, Boolean> permissions) {
        other.permissions = permissions;
    }

    @Override
    public boolean isActionAllowed(RequestedAction action) {
        if (action == RequestedAction.READ) {
            return isVisible(user).test(document);
        } else if (action == RequestedAction.WRITE_ECC) {
            Set<UserGroup> allSecRoles = !CommonUtils.isNullOrEmptyMap(user.getSecondaryDepartmentsAndRoles())
                    ? user.getSecondaryDepartmentsAndRoles().entrySet().stream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.toSet())
                    : new HashSet<UserGroup>();
            return PermissionUtils.isUserAtLeast(UserGroup.ECC_ADMIN, user)
                    || PermissionUtils.isUserAtLeastDesiredRoleInSecondaryGroup(UserGroup.ECC_ADMIN, allSecRoles);
        } else {
            return getStandardPermissions(action);
        }
    }

    @Override
    protected Set<String> getContributors() {
        return contributors;
    }

    @Override
    protected Set<String> getModerators() {
        return moderators;
    }

    @Override
    protected Set<String> getAttachmentContentIds() {
        return attachmentContentIds;
    }

    protected Set<String> getUserEquivalentOwnerGroup() {
        Set<String> departments = new HashSet<String>();
        departments.add(user.getDepartment());
        if (!CommonUtils.isNullOrEmptyMap(user.getSecondaryDepartmentsAndRoles())) {
            departments.addAll(user.getSecondaryDepartmentsAndRoles().keySet());
        }

        return departments;
    }
}
