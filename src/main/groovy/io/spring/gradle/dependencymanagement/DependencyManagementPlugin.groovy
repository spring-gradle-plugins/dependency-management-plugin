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

package io.spring.gradle.dependencymanagement

import io.spring.gradle.dependencymanagement.DependencyManagementExtension.PomCustomizationConfiguration
import io.spring.gradle.dependencymanagement.exclusions.ExclusionConfiguringAction
import io.spring.gradle.dependencymanagement.exclusions.ExclusionResolver
import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder
import io.spring.gradle.dependencymanagement.maven.PomDependencyManagementConfigurer
import io.spring.gradle.dependencymanagement.report.DependencyManagementReportTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.maven.PomFilterContainer
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.Upload
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Main class for the dependency management plugin
 *
 * @author Andy Wilkinson
 */
class DependencyManagementPlugin implements Plugin<Project> {

    private final Logger log = LoggerFactory.getLogger(DependencyManagementPlugin)

    @Override
    void apply(Project project) {
        DependencyManagementContainer dependencyManagementContainer =
                new DependencyManagementContainer(project)

        DependencyManagementExtension extension = project.extensions.create("dependencyManagement",
                DependencyManagementExtension, dependencyManagementContainer, project)

        project.tasks.create("dependencyManagement", DependencyManagementReportTask) { task ->
            task.dependencyManagement = dependencyManagementContainer
        }

        project.configurations.all { Configuration root ->
            root.incoming.beforeResolve {
				if (extension.overriddenByDependencies) {
	                root.hierarchy.each { Configuration configuration ->
	                    configuration.incoming.dependencies.findAll {
	                        it in ModuleDependency && it.version
	                    }.each {
	                        log.debug(
	                                "Adding managed version in configuration '{}' for dependency '{}'",
	                                configuration.name, it)
	                        dependencyManagementContainer.
	                                addImplicitManagedVersion(configuration, it.group, it.name,
	                                        it.version)
	                    }
	                }
				}
            }
        }

        ExclusionResolver exclusionResolver = new ExclusionResolver(project.dependencies,
                project.configurations, new EffectiveModelBuilder(project))

        project.configurations.all { Configuration c ->
            log.info("Applying dependency management to configuration '{}' in project '{}'",
                    c.name, project.name)

            VersionConfiguringAction versionConfiguringAction = new VersionConfiguringAction(project,
                    dependencyManagementContainer, c)

            c.incoming.beforeResolve(new ExclusionConfiguringAction(
                    project.extensions.findByType(DependencyManagementExtension),
                    dependencyManagementContainer, project.configurations, c, exclusionResolver,
                    versionConfiguringAction))

            resolutionStrategy.eachDependency(versionConfiguringAction)
        }

        project.tasks.withType(Upload).all { uploadTask ->
            uploadTask.repositories.withType(PomFilterContainer).all { container ->
                configurePom(container.pom, extension.pomConfigurer)
            }
        }

        project.plugins.withType(MavenPublishPlugin) {
            project.extensions.configure(PublishingExtension, new ClosureBackedAction({
                publications.withType(MavenPublication).all { publication ->
                    configurePom(publication.pom, extension.pomConfigurer)
                }
            }))
        }
    }

    private void configurePom(def pom, PomDependencyManagementConfigurer configurer) {
        pom.withXml { xml -> configurer.configurePom(xml.asNode()) }
    }

}