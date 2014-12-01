/*
 * Copyright 2014 the original author or authors.
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

package io.spring.gradle.dependencymanagement

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.mvn3.org.apache.maven.model.Model
import org.gradle.mvn3.org.apache.maven.model.Repository
import org.gradle.mvn3.org.apache.maven.model.building.*
import org.gradle.mvn3.org.apache.maven.model.interpolation.StringSearchModelInterpolator
import org.gradle.mvn3.org.apache.maven.model.path.DefaultPathTranslator
import org.gradle.mvn3.org.apache.maven.model.path.DefaultUrlNormalizer
import org.gradle.mvn3.org.apache.maven.model.resolution.ModelResolver
import org.gradle.mvn3.org.apache.maven.model.resolution.UnresolvableModelException
import org.gradle.mvn3.org.codehaus.plexus.interpolation.MapBasedValueSource
import org.gradle.mvn3.org.codehaus.plexus.interpolation.PropertiesBasedValueSource
import org.gradle.mvn3.org.codehaus.plexus.interpolation.ValueSource
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Encapsulates dependency management information for a particular configuration in a Gradle project
 * 
 * @author Andy Wilkinson
 */
class DependencyManagement {

    private final Logger log = LoggerFactory.getLogger(DependencyManagement)

    private final Project project

    private final Configuration configuration

    private final Configuration targetConfiguration

    private boolean resolved

    private Map versions = [:]

    def DependencyManagement(Project project) {
        this(project, null)
    }

    def DependencyManagement(Project project, Configuration targetConfiguration) {
        this.project = project
        this.configuration = this.project.configurations.detachedConfiguration()
        this.targetConfiguration = targetConfiguration
    }

    void importBom(bomCoordinates) {
        configuration.dependencies.add(project.dependencies.create(bomCoordinates + '@pom'))
    }

    void addManagedVersion(String group, String name, String version) {
        versions[createKey(group, name)] = version;
    }

    String getManagedVersion(String group, String name) {
        resolveIfNecessary()
        versions[createKey(group, name)]
    }

    private String createKey(String group, String name) {
        "$group:$name"
    }

    boolean apply(DependencyResolveDetails details) {
        String version = getManagedVersion(details.requested.group, details.requested.name)
        if (version) {
            details.useVersion(version)
            true
        }
        else {
            false
        }
    }

    private void resolveIfNecessary() {
        if (!resolved) {
            resolve()
        }
        resolved = true
    }

    private void resolve() {
        if (targetConfiguration) {
            log.info("Resolving dependency management for configuration '{}' of project '{}'",
                    targetConfiguration.name, project.name)
        }
        else {
            log.info("Resolving global dependency management for project '{}'", project.name)
        }
        def existingVersions = [:]
        existingVersions << versions

        log.debug("Preserving existing versions: {}", existingVersions)

        def modelBuilder = new DefaultModelBuilderFactory().newInstance()
        modelBuilder.modelInterpolator = new ProjectPropertiesModelInterpolator(project)

        configuration.resolve().each { File file ->
            log.debug("Processing '{}'", file)
            def request = new DefaultModelBuildingRequest()
            request.setSystemProperties(System.getProperties())
            request.setModelSource(new FileModelSource(file))
            request.modelResolver = new StandardModelResolver()
            try {
                def result = modelBuilder.build(request)
                def errors = extractErrors(result.problems)
                if (errors) {
                    reportErrors(errors, file)
                }
                else {
                    result.effectiveModel.dependencyManagement.dependencies.each { dependency ->
                        versions["$dependency.groupId:$dependency.artifactId" as String
                                ] = dependency.version
                    }
                }
            }
            catch (ModelBuildingException ex) {
                reportErrors(extractErrors(ex.problems), file)
            }
        }

        versions << existingVersions

        log.info("Resolved versions: {}", versions)
    }

    private List<ModelProblem> extractErrors(List<ModelProblem> problems) {
        problems.findAll { it.severity == ModelProblem.Severity.ERROR }
    }

    private void reportErrors(List<ModelProblem> errors, File file) {
        def errorMessages = errors.collect {
            ModelProblem problem -> "\n    $problem.message in $problem.modelId"
        } as Set
        String message = "Processing of $file.name failed. Its dependency management will be unavailable:"
        errorMessages.each { message += it }
        log.error(message)
    }

    private static class ProjectPropertiesModelInterpolator extends StringSearchModelInterpolator {

        private final Project project

        ProjectPropertiesModelInterpolator(Project project) {
            this.project = project
            setUrlNormalizer(new DefaultUrlNormalizer());
            setPathTranslator(new DefaultPathTranslator());
        }

        List<ValueSource> createValueSources(Model model, File projectDir,
                ModelBuildingRequest request, ModelProblemCollector collector) {
            List valueSources = [
                    new MapBasedValueSource(project.properties),
                    new PropertiesBasedValueSource(System.getProperties())]
            valueSources.addAll(super.createValueSources(model, projectDir, request, collector))
            valueSources
        }
    }

    private class StandardModelResolver implements ModelResolver {

        @Override
        ModelSource resolveModel(String groupId, String artifactId, String version)
                throws UnresolvableModelException {
            def dependency = project.dependencies.create("$groupId:$artifactId:$version@pom")
            def configuration = project.configurations.detachedConfiguration(dependency)
            new FileModelSource(configuration.resolve().iterator().next())
        }

        @Override
        void addRepository(Repository repository) {
        }

        @Override
        ModelResolver newCopy() {
            this
        }
    }
}
