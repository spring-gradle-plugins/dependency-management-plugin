/*
 * Copyright 2014-2017 the original author or authors.
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

package io.spring.gradle.dependencymanagement.internal;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.spring.gradle.dependencymanagement.internal.pom.PomResolver;

/**
 * An {@link Action} that applies dependency management to a {@link Project}.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementApplier implements Action<Configuration> {

    private static final Logger logger = LoggerFactory.getLogger(DependencyManagementApplier.class);

    private final Project project;

    private final ExclusionResolver exclusionResolver;

    private final DependencyManagementContainer dependencyManagementContainer;

    private final DependencyManagementConfigurationContainer configurationContainer;

    private final DependencyManagementSettings dependencyManagementSettings;

    /**
     * Creates a new {@code DependencyManagementApplier} that will apply dependency management to the given
     * {@code project}.
     *
     * @param project the project
     * @param dependencyManagementContainer the container for the project's dependency management
     * @param configurationContainer the container for dependency management-specific configurations
     * @param dependencyManagementSettings settings that control who dependency management is applied
     * @param pomResolver used to perform any necessary pom resolution while applying dependency management
     */
    public DependencyManagementApplier(Project project, DependencyManagementContainer dependencyManagementContainer,
            DependencyManagementConfigurationContainer configurationContainer,
            DependencyManagementSettings dependencyManagementSettings, PomResolver pomResolver) {
        this.project = project;
        this.exclusionResolver = new ExclusionResolver(pomResolver);
        this.dependencyManagementContainer = dependencyManagementContainer;
        this.configurationContainer = configurationContainer;
        this.dependencyManagementSettings = dependencyManagementSettings;
    }

    @Override
    public void execute(Configuration configuration) {
        logger.debug("Applying dependency management to configuration '{}' in project '{}'",
                configuration.getName(), this.project.getName());

        final VersionConfiguringAction versionConfiguringAction = new VersionConfiguringAction(
                this.project, this.dependencyManagementContainer, configuration);

        configuration.getIncoming().beforeResolve(new ExclusionConfiguringAction(this.dependencyManagementSettings,
                this.dependencyManagementContainer, this.configurationContainer, configuration,
                this.exclusionResolver, new DependencyManagementConfigurationContainer.ConfigurationConfigurer() {

            @Override
            public void configure(Configuration configuration) {
                configuration.getResolutionStrategy().eachDependency(versionConfiguringAction);
            }
        }));

        configuration.getResolutionStrategy().eachDependency(versionConfiguringAction);
    }
}
