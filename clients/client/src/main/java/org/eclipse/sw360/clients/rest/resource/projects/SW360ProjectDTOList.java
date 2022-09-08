package org.eclipse.sw360.clients.rest.resource.projects;

import org.eclipse.sw360.clients.rest.resource.LinkObjects;
import org.eclipse.sw360.clients.rest.resource.SW360HalResource;


public class SW360ProjectDTOList extends SW360HalResource<LinkObjects, SW360ProjectDTOListEmbedded> {

    @Override
    public LinkObjects createEmptyLinks() {
        return new LinkObjects();
    }

    @Override
    public SW360ProjectDTOListEmbedded createEmptyEmbedded() {
        return new SW360ProjectDTOListEmbedded();
    }
}
