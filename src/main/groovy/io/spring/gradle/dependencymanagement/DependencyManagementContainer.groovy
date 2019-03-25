/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Container object for a Gradle build project's dependency management, handling the project's global and
 * configuration-specific dependency management
 * @author Andy Wilkinson
 */
class DependencyManagementContainer {

    private final Logger log = LoggerFactory.getLogger(DependencyManagementContainer)

    private final DependencyManagementConfigurationContainer configurationContainer

    protected final DependencyManagement globalDependencyManagement

    private final EffectiveModelBuilder effectiveModelBuilder

    final Project project

    private final Map<Configuration, DependencyManagement> configurationDependencyManagement = [:]

    DependencyManagementContainer(Project project,
            DependencyManagementConfigurationContainer configurationContainer,
            EffectiveModelBuilder effectiveModelBuilder) {
        this.project = project
        this.configurationContainer = configurationContainer
        this.globalDependencyManagement = new DependencyManagement(this.project,
                configurationContainer.newConfiguration(), effectiveModelBuilder)
        this.effectiveModelBuilder = effectiveModelBuilder
    }

    void addImplicitManagedVersion(configuration, String group, String name, String version) {
        dependencyManagementForConfiguration(configuration).addImplicitManagedVersion(group,
                name, version)
    }

    void addExplicitManagedVersion(configuration, String group, String name, String version,
            List<String> exclusions) {
        dependencyManagementForConfiguration(configuration).addExplicitManagedVersion(group,
                name, version, exclusions)
    }

    void importBom(Configuration configuration, String coordinates, Map<String, String> properties) {
        dependencyManagementForConfiguration(configuration).importBom(coordinates, properties)
    }

    String getManagedVersion(Configuration configuration, String group, String name) {
        String version = null
        if (configuration) {
            version = configuration.hierarchy.findResult {
                def managedVersion =
                        dependencyManagementForConfiguration(it).getManagedVersion(group, name)
                if (managedVersion) {
                    log.debug(
                            "Found managed version '{}' for dependency '{}:{}' in dependency management for configuration '{}'",
                            managedVersion, group, name, it.name)
                }
                managedVersion
            }
        }
        if (version == null) {
            version = globalDependencyManagement.getManagedVersion(group, name)
            if (version) {
                log.debug(
                        "Found managed version '{}' for dependency '{}:{}' in global dependency management",
                        version, group, name)
            }
        }
        version
    }

    Exclusions getExclusions(Configuration configuration) {
        Exclusions exclusions = new Exclusions()
        if (configuration) {
            configuration.hierarchy.each {
                exclusions.addAll(dependencyManagementForConfiguration(it).getExclusions())
            }
        }
        exclusions.addAll(globalDependencyManagement.getExclusions())
        exclusions
    }

    Properties importedPropertiesForConfiguration(Configuration configuration) {
        Properties properties = new Properties()
        properties.putAll(globalDependencyManagement.importedProperties)
        if (configuration) {
            (configuration.hierarchy as List).reverse().each {
                properties.putAll(dependencyManagementForConfiguration(it).importedProperties)
            }
        }
        properties
    }

    def managedVersionsForConfiguration(Configuration configuration) {
        managedVersionsForConfiguration(configuration, true)
    }

    def managedVersionsForConfiguration(Configuration configuration, boolean inherited) {
        if (inherited) {
            def managedVersions = dependencyManagementForConfiguration(null).managedVersions
            if (configuration) {
                new ArrayList(configuration.hierarchy).reverseEach { Configuration c ->
                    managedVersions.putAll(dependencyManagementForConfiguration(c).managedVersions)
                }
            }
            return managedVersions
        }
        dependencyManagementForConfiguration(configuration).managedVersions
    }


    private DependencyManagement dependencyManagementForConfiguration(
            Configuration configuration) {
        if (!configuration) {
            globalDependencyManagement
        }
        else {
            DependencyManagement dependencyManagement = configurationDependencyManagement.get(configuration)
            if (!dependencyManagement) {
                Configuration dependencyManagementConfiguration = this.configurationContainer.newConfiguration()
                dependencyManagement = new DependencyManagement(project, configuration,
                        dependencyManagementConfiguration, effectiveModelBuilder)
                configurationDependencyManagement.put(configuration, dependencyManagement)
            }
            dependencyManagement
        }
    }

}
