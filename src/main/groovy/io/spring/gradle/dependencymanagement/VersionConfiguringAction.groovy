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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * An {@link Action} to be applied to {@link DependencyResolveDetails} that configures
 * the dependency's version based on the dependency management.
 *
 * @author Andy Wilkinson
 */
class VersionConfiguringAction implements Action<DependencyResolveDetails> {

    private final Logger log = LoggerFactory.getLogger(VersionConfiguringAction)

    private final Project project

    private final DependencyManagementContainer dependencyManagementContainer

    private final Configuration configuration

    public VersionConfiguringAction(Project project, DependencyManagementContainer
            dependencyManagementContainer, Configuration configuration) {
        this.project = project
        this.dependencyManagementContainer = dependencyManagementContainer
        this.configuration = configuration
    }

    @Override
    void execute(DependencyResolveDetails details) {
        log.debug("Processing dependency '{}'", details.requested)
        if (isDependencyOnLocalProject(project, details)) {
            log.debug("'{}' is a local project dependency. Dependency management has not been " +
                    "applied", details.requested)
            return;
        }
        if (Versions.isDynamic(details.requested.version)) {
            log.info("'{}' has a dynamic version. Dependency management has not been applied",
                    details.requested)
            return;
        }
        String version = dependencyManagementContainer.
                getManagedVersion(configuration, details.requested.group,
                        details.requested.name)
        if (version && version != details.requested.version) {
            log.info("Using version '{}' for dependency '{}'", version,
                    details.requested)
            details.useVersion(version)
        }
        else {
            log.debug("No dependency management for dependency '{}'",
                    details.requested)
        }
    }

    private static boolean isDependencyOnLocalProject(Project project,
            DependencyResolveDetails details) {
        project.rootProject.allprojects
                .collect { "$it.group:$it.name" as String }
                .contains("$details.requested.group:$details.requested.name" as String)
    }

}
