/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement.internal.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuilder;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuilderFactory;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuildingRequest;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.FileModelSource;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingException;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingResult;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelProblem;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.resolution.ModelResolver;

/**
 * Builds the effective {@link Model} for a Maven pom.
 *
 * @author Andy Wilkinson
 */
final class EffectiveModelBuilder {

    private static final Logger logger = LoggerFactory.getLogger(EffectiveModelBuilder.class);

    private final Project project;

    private final ModelResolver modelResolver;

    EffectiveModelBuilder(Project project,
            DependencyManagementConfigurationContainer configurationContainer) {
        this.project = project;
        this.modelResolver = new ConfigurationModelResolver(project, configurationContainer);
    }

    Model buildModel(File pom, Map<String, String> properties) {
        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
        request.setSystemProperties(System.getProperties());
        request.setModelSource(new FileModelSource(pom));
        request.setModelResolver(this.modelResolver);

        try {
            ModelBuildingResult result = createModelBuilder(this.project, properties).build(request);
            List<ModelProblem> errors = extractErrors(result.getProblems());
            if (errors.isEmpty()) {
                return result.getEffectiveModel();
            }
            reportErrors(errors, pom);

        }
        catch (ModelBuildingException ex) {
            logger.debug("Model building failed", ex);
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
        logger.error(message.toString());
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
