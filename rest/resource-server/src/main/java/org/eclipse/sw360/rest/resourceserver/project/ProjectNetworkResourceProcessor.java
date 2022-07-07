package org.eclipse.sw360.rest.resourceserver.project;

import lombok.RequiredArgsConstructor;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectDTO;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectNetwork;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelProcessor;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

@Component
@RequiredArgsConstructor
public class ProjectNetworkResourceProcessor implements RepresentationModelProcessor<EntityModel<ProjectNetwork>> {

    @Override
    public EntityModel<ProjectNetwork> process(EntityModel<ProjectNetwork> model) {
        ProjectNetwork project = model.getContent();
        Link selfLink = linkTo(ProjectController.class)
                .slash("api" + ProjectController.PROJECTS_URL + "/dependency/" + project.getId()).withSelfRel();
        model.add(selfLink);
        return model;
    }
}
