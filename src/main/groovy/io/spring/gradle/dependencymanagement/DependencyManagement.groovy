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

package io.spring.gradle.dependencymanagement

import io.spring.gradle.dependencymanagement.exclusions.Exclusions
import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder
import io.spring.gradle.dependencymanagement.maven.ModelExclusionCollector
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
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

    private final EffectiveModelBuilder effectiveModelBuilder

    private boolean resolved

    private Map<String, String> versions = [:]

    private Map<String, String> explicitVersions = [:]

    private Exclusions explicitExclusions = new Exclusions()

    private Exclusions allExclusions = new Exclusions()

    private Map bomDependencyManagement = [:] as LinkedHashMap

    private Properties bomProperties = new Properties()

    private List<Dependency> importedBoms = [];

    def DependencyManagement(Project project, Configuration dependencyManagementConfiguration,
            EffectiveModelBuilder effectiveModelBuilder) {
        this(project, null, dependencyManagementConfiguration, effectiveModelBuilder)
    }

    def DependencyManagement(Project project, Configuration targetConfiguration, Configuration
            dependencyManagementConfiguration, EffectiveModelBuilder effectiveModelBuilder) {
        this.project = project
        this.configuration = dependencyManagementConfiguration
        this.targetConfiguration = targetConfiguration
        this.effectiveModelBuilder = effectiveModelBuilder
    }

    void importBom(bomCoordinates) {
        importedBoms << project.dependencies.create(bomCoordinates + '@pom')
    }

    Map getImportedBoms() {
        resolveIfNecessary()
        bomDependencyManagement
    }

    Properties getImportedProperties() {
        resolveIfNecessary()
        bomProperties
    }

    void addManagedVersion(String group, String name, String version) {
        versions[createKey(group, name)] = version
    }

    void addImplicitManagedVersion(String group, String name, String version) {
        addManagedVersion(group, name, version)
    }

    void addExplicitManagedVersion(String group, String name, String version, List<String>
            exclusions) {
        def key = createKey(group, name)
        explicitVersions[key] = version
        explicitExclusions.add(key, exclusions)
        allExclusions.add(key, exclusions)
        addManagedVersion(group, name, version)
    }

    String getManagedVersion(String group, String name) {
        resolveIfNecessary()
        versions[createKey(group, name)]
    }

    Map getManagedVersions() {
        resolveIfNecessary()
        return new HashMap(versions)
    }

    void explicitManagedVersions(Closure closure) {
        explicitVersions.each { key, value ->
            def (groupId, artifactId) = key.split(':')
            closure.call(groupId, artifactId, value, explicitExclusions.exclusionsForDependency(key))
        }
    }

    private String createKey(String group, String name) {
        "$group:$name"
    }

    Exclusions getExclusions() {
        resolveIfNecessary()
        allExclusions
    }

    private void resolveIfNecessary() {
        if (!resolved) {
            try {
                resolved = true
                resolve()
            } catch (Exception ex) {
                throw new GradleException("Failed to resolve imported Maven boms:" +
                        " ${getRootCause(ex).message}", ex)
            }
        }
    }

    private Throwable getRootCause(Exception ex) {
        Throwable candidate = ex;
        while(candidate.cause) {
            candidate = candidate.cause
        }
        return candidate
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

        importedBoms.each { configuration.dependencies.add(it) }

        Map<String, File> artifacts = configuration.resolvedConfiguration.resolvedArtifacts.collectEntries {
            [("${it.moduleVersion.id.group}:${it.moduleVersion.id.name}" as String) : it.file]}

        importedBoms.each {
            File file = artifacts["${it.group}:${it.name}" as String]
            log.debug("Processing '{}'", file)
            Model effectiveModel = this.effectiveModelBuilder.buildModel(file)
            if (effectiveModel.dependencyManagement?.dependencies) {
                String bomCoordinates = "${effectiveModel.groupId}:${effectiveModel.artifactId}:${effectiveModel.version}"
                effectiveModel.dependencyManagement.dependencies.each { dependency ->
                    versions["$dependency.groupId:$dependency.artifactId" as String] = dependency.version
                }
                bomDependencyManagement[bomCoordinates] = effectiveModel.dependencyManagement.dependencies
                allExclusions.addAll(
                        new ModelExclusionCollector().collectExclusions(effectiveModel))
            }
            if (effectiveModel.properties) {
                bomProperties.putAll(effectiveModel.properties)
            }
        }

        versions << existingVersions

        log.info("Resolved versions: {}", versions)
    }
}
