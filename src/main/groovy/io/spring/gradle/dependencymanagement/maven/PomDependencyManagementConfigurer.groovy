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

package io.spring.gradle.dependencymanagement.maven

import io.spring.gradle.dependencymanagement.DependencyManagement

/**
 * Configures the dependency management in a Maven pom produced as part of a Gradle build
 *
 * @author Andy Wilkinson
 */
class PomDependencyManagementConfigurer {

    private DependencyManagement dependencyManagement

    PomDependencyManagementConfigurer(DependencyManagement dependencyManagement) {
        this.dependencyManagement = dependencyManagement
    }

    void configurePom(Node pom) {
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
        this.dependencyManagement.importedBoms.each { bomCoordinates ->
            def (groupId, artifactId, version) = bomCoordinates.split(':')
            def dependency = dependencies.appendNode('dependency')
            dependency.appendNode('groupId').value = groupId
            dependency.appendNode('artifactId').value = artifactId
            dependency.appendNode('version').value = version
            dependency.appendNode('scope').value = 'import'
            dependency.appendNode('type').value = 'pom'
        }
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


