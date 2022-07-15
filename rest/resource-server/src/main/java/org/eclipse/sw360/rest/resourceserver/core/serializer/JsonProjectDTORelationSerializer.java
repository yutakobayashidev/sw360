package org.eclipse.sw360.rest.resourceserver.core.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.eclipse.sw360.datahandler.thrift.projects.ProjectProjectRelationship;
import org.eclipse.sw360.rest.resourceserver.project.ProjectController;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
@Component
public class JsonProjectDTORelationSerializer  extends JsonSerializer<Map<String, ProjectProjectRelationship>> {
    @Override
    public void serialize(Map<String, ProjectProjectRelationship> projectRelationMap, JsonGenerator gen, SerializerProvider provider)
            throws IOException {

        List<Map<String, String>> linkedProjects = new ArrayList<>();
        for (Map.Entry<String, ProjectProjectRelationship> projectRelation : projectRelationMap.entrySet()) {
            String projectLink = linkTo(ProjectController.class).slash("api" +
                    ProjectController.PROJECTS_URL + "/dependency/" + projectRelation.getKey()).withSelfRel().getHref();

            Map<String, String> linkedProject = new HashMap<>();
            linkedProject.put("relation", projectRelation.getValue().getProjectRelationship().name());
            linkedProject.put("enableSvm", String.valueOf(projectRelation.getValue().isEnableSvm()));
            linkedProject.put("project", projectLink);
            linkedProjects.add(linkedProject);

        }
        gen.writeObject(linkedProjects);
    }
}
