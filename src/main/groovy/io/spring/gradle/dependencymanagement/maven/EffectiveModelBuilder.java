package io.spring.gradle.dependencymanagement.maven;

import io.spring.gradle.dependencymanagement.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuilder;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuilderFactory;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuildingRequest;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.FileModelSource;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingException;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingResult;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelProblem;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.resolution.ModelResolver;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds the effective {@link Model} for a Maven pom
 *
 * @author Andy Wilkinson
 */
public final class EffectiveModelBuilder {

    private final Logger log = LoggerFactory.getLogger(EffectiveModelBuilder.class);

    private final Project project;

    private final ModelResolver modelResolver;

    public EffectiveModelBuilder(Project project,
            DependencyManagementConfigurationContainer configurationContainer) {
        this.project = project;
        this.modelResolver = new PomDependencyModelResolver(project, configurationContainer);
    }

    public Model buildModel(File pom) {
        return this.buildModel(pom, Collections.<String, String>emptyMap());
    }

    public Model buildModel(File pom, Map<String, String> properties) {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setSystemProperties(System.getProperties());
        request.setModelSource(new FileModelSource(pom));
        request.setModelResolver(modelResolver);

        try {
            ModelBuildingResult result = createModelBuilder(this.project, properties).build(request);
            List<ModelProblem> errors = extractErrors(result.getProblems());
            if (errors.isEmpty()) {
                return result.getEffectiveModel();
            }
            reportErrors(errors, pom);

        }
        catch (ModelBuildingException ex) {
            reportErrors(extractErrors(ex.getProblems()), pom);
        }

        return null;
    }

    private List<ModelProblem> extractErrors(List<ModelProblem> problems) {
        List<ModelProblem> errors = new ArrayList<ModelProblem>();
        for (ModelProblem problem: problems) {
            if (problem.getSeverity() == ModelProblem.Severity.ERROR) {
                errors.add(problem);
            }
        }
        return errors;
    }

    private void reportErrors(List<ModelProblem> errors, File file) {
        StringBuilder message = new StringBuilder("Processing of " + file + " failed:");
        for (ModelProblem error: errors) {
            message.append("\n    " + error.getMessage() + " in " + error.getModelId());
        }
        log.error(message.toString());
    }

    private DefaultModelBuilder createModelBuilder(Project project,
            Map<String, String> properties) {
        DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
        modelBuilder
                .setModelInterpolator(new ProjectPropertiesModelInterpolator(project, properties));
        modelBuilder.setModelValidator(new RelaxedModelValidator());
        return modelBuilder;
    }

}
