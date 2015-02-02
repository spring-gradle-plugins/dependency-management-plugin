/*
 * Copyright 2014-2015 the original author or authors.
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

import io.spring.gradle.dependencymanagement.DependencyManagement
import io.spring.gradle.dependencymanagement.DependencyManagementExtension.PomCustomizationConfiguration
import io.spring.gradle.dependencymanagement.DependencyManagementExtension.PomCustomizationConfiguration.ImportedBomAction

/**
 * Configures the dependency management in a Maven pom produced as part of a Gradle build
 *
 * @author Andy Wilkinson
 */
class PomDependencyManagementConfigurer {

    private DependencyManagement dependencyManagement

    PomCustomizationConfiguration configuration

    PomDependencyManagementConfigurer(DependencyManagement dependencyManagement,
            PomCustomizationConfiguration configuration) {
        this.dependencyManagement = dependencyManagement
        this.configuration = configuration
    }

    void configurePom(Node pom) {
        if (configuration.enabled) {
            doConfigurePom(pom)
        }
    }

    private void doConfigurePom(Node pom) {
        def dependencyManagement = pom.dependencyManagement
        if (!pom.dependencyManagement) {
            dependencyManagement = pom.appendNode('dependencyManagement')
        }
        def dependencies = dependencyManagement.dependencies
        if (!dependencies) {
            dependencies = dependencyManagement.appendNode('dependencies')
        }
        configureBomImports(dependencies)
        configureDependencies(dependencies)
    }

    private void configureBomImports(Node dependencies) {
        this.dependencyManagement.importedBoms.each { String bomCoordinates, bomDependencies ->
            if (configuration.importedBomAction == ImportedBomAction.IMPORT) {
                addImport(dependencies, bomCoordinates);
            } else {
                bomDependencies.each { dependency -> addDependency(dependencies, dependency) }
            }
        }
    }

    private void addImport(Node dependencies, String bomCoordinates) {
        def (groupId, artifactId, version) = bomCoordinates.split(':')
        def dependency = dependencies.appendNode('dependency')
        dependency.appendNode('groupId').value = groupId
        dependency.appendNode('artifactId').value = artifactId
        dependency.appendNode('version').value = version
        dependency.appendNode('scope').value = 'import'
        dependency.appendNode('type').value = 'pom'
    }

    private void addDependency(Node dependencies, def dependencyToAdd) {
        def dependency = dependencies.appendNode('dependency')
        dependency.appendNode('groupId').value = dependencyToAdd.groupId
        dependency.appendNode('artifactId').value = dependencyToAdd.artifactId
        dependency.appendNode('version').value = dependencyToAdd.version
        if (dependencyToAdd.type != 'jar') {
            dependency.appendNode('type').value = dependencyToAdd.type
        }
        if (dependencyToAdd.classifier) {
            dependency.appendNode('classifier').value = dependencyToAdd.classifier
        }
        if (dependencyToAdd.scope) {
            dependency.appendNode('scope').value = dependencyToAdd.scope
        }
        if (dependencyToAdd.exclusions) {
            def exclusions = dependency.appendNode('exclusions')
            dependencyToAdd.exclusions.each { addExclusion(exclusions, dependencyToAdd.exclusions) }
        }
    }

    private void addExclusion(Node exclusions, def exclusionToAdd) {
        def exclusion = exclusions.appendNode('exclusion')
        exclusion.appendNode('groupId').value = exclusionToAdd.groupId
        exclusion.appendNode('artifactId').value = exclusionToAdd.artifactId
    }

    private void configureDependencies(Node dependencies) {
        this.dependencyManagement.explicitManagedVersions { groupId, artifactId, version ->
            def dependency = dependencies.appendNode('dependency')
            dependency.appendNode('groupId').value = groupId
            dependency.appendNode('artifactId').value = artifactId
            dependency.appendNode('version').value = version
        }
    }
}
