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

import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.Dependency

/**
 * A container for {@link Configuration Configurations} created by the dependency management plugin
 * that aren't part of the project's configurations.
 *
 * @author Andy Wilkinson
 */
class DependencyManagementConfigurationContainer {

    private final DomainObjectCollection<Configuration> configurations

    private final ConfigurationContainer delegate

    DependencyManagementConfigurationContainer(Project project) {
        this.delegate = project.configurations
        this.configurations = project.container(Configuration)
    }

    Configuration newConfiguration(Dependency... dependencies) {
        Configuration configuration = delegate.detachedConfiguration(dependencies)
        configurations.add(configuration)
        return configuration
    }

    void resolutionStrategy(Closure closure) {
        this.configurations.all { Configuration configuration ->
            configuration.resolutionStrategy closure
        }
    }

}
