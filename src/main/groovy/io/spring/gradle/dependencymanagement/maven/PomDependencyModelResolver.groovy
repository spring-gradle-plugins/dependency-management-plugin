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
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Repository
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.FileModelSource
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelSource
import io.spring.gradle.dependencymanagement.org.apache.maven.model.resolution.ModelResolver
import io.spring.gradle.dependencymanagement.org.apache.maven.model.resolution.UnresolvableModelException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

/**
 * A {@link ModelResolver} that uses a {@link Configuration} to resolve a pom dependency for the
 * requested model
 *
 * @author Andy Wilkinson
 */
class PomDependencyModelResolver implements ModelResolver {

    private final Project project

    private final DependencyManagementConfigurationContainer configurationContainer

    private Map<String, FileModelSource> pomCache = [:]

    PomDependencyModelResolver(Project project,
            DependencyManagementConfigurationContainer configurationContainer) {
        this.project = project
        this.configurationContainer = configurationContainer
    }

    @Override
    ModelSource resolveModel(String groupId, String artifactId, String version)
            throws UnresolvableModelException {
        def id = "$groupId:$artifactId:$version@pom"

        def pom = pomCache[id]

        if (!pom) {
            def dependency = project.dependencies.create("$groupId:$artifactId:$version@pom")
            def configuration = configurationContainer.newConfiguration(dependency)
            pom = new FileModelSource(configuration.resolve().iterator().next())
            pomCache[id] = pom
        }

        pom
    }

    @Override
    void addRepository(Repository repository) {
    }

    @Override
    ModelResolver newCopy() {
        this
    }
}
