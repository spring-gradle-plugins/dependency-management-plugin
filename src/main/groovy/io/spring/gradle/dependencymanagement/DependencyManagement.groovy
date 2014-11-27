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

import io.spring.gradle.dependencymanagement.exclusions.Exclusions
import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder
import io.spring.gradle.dependencymanagement.maven.ModelExclusionCollector
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.mvn3.org.apache.maven.model.Model
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

    private Exclusions bomExclusions = new Exclusions()

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

    Exclusions getExclusions() {
        resolveIfNecessary()
        bomExclusions
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

        def effectiveModelBuilder = new EffectiveModelBuilder(project)

        configuration.resolve().each { File file ->
            log.debug("Processing '{}'", file)
            Model effectiveModel = effectiveModelBuilder.buildModel(file)
            if (effectiveModel) {
                effectiveModel.dependencyManagement.dependencies.each { dependency ->
                    versions["$dependency.groupId:$dependency.artifactId" as String
                            ] = dependency.version
                }
                bomExclusions.addAll(
                        new ModelExclusionCollector().collectExclusions(effectiveModel))
            }
        }

        versions << existingVersions

        log.info("Resolved versions: {}", versions)
    }
}
