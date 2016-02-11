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

package io.spring.gradle.dependencymanagement.maven

import io.spring.gradle.dependencymanagement.DependencyManagementConfigurationContainer
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuilder
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuilderFactory
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuildingRequest
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.FileModelSource
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingException
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelProblem
import io.spring.gradle.dependencymanagement.org.apache.maven.model.resolution.ModelResolver

import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Builds the effective {@link Model} for a Maven pom
 *
 * @author Andy Wilkinson
 */
class EffectiveModelBuilder {

    private final Logger log = LoggerFactory.getLogger(EffectiveModelBuilder)

    private final DefaultModelBuilder mavenModelBuilder

    private final ModelResolver modelResolver

    EffectiveModelBuilder(Project project,
            DependencyManagementConfigurationContainer configurationContainer) {
        mavenModelBuilder = new DefaultModelBuilderFactory().newInstance()
        mavenModelBuilder.modelInterpolator = new ProjectPropertiesModelInterpolator(
                project)
        mavenModelBuilder.modelValidator = new RelaxedModelValidator();
        modelResolver = new PomDependencyModelResolver(project, configurationContainer)
    }

    Model buildModel(File pom) {
        def request = new DefaultModelBuildingRequest()
        request.setSystemProperties(System.getProperties())
        request.setModelSource(new FileModelSource(pom))
        request.modelResolver = modelResolver

        try {
            def result = mavenModelBuilder.build(request)
            def errors = extractErrors(result.problems)
            if (errors) {
                reportErrors(errors, pom)
            }
            else {
                result.effectiveModel
            }
        }
        catch (ModelBuildingException ex) {
            reportErrors(extractErrors(ex.problems), pom)
        }
    }

    private List<ModelProblem> extractErrors(List<ModelProblem> problems) {
        problems.findAll { it.severity == ModelProblem.Severity.ERROR }
    }

    private void reportErrors(List<ModelProblem> errors, File file) {
        def errorMessages = errors.collect {
            ModelProblem problem -> "\n    $problem.message in $problem.modelId"
        } as Set
        String message = "Processing of $file failed:"
        errorMessages.each { message += it }
        log.error(message)
    }
}
