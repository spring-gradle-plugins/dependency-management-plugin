/*
 * Copyright 2014-2022 the original author or authors.
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

package io.spring.gradle.dependencymanagement.internal.bridge;

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementApplier;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings;
import io.spring.gradle.dependencymanagement.internal.ImplicitDependencyManagementCollector;
import io.spring.gradle.dependencymanagement.internal.dsl.StandardDependencyManagementExtension;
import io.spring.gradle.dependencymanagement.internal.maven.MavenPomResolver;
import io.spring.gradle.dependencymanagement.internal.report.DependencyManagementReportTask;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.HelpTasksPlugin;

/**
 * Creates and provides access to the plugin's internal components.
 *
 * @author Andy Wilkinson
 */
public class InternalComponents {

	private final Project project;

	private final DependencyManagementExtension dependencyManagementExtension;

	private final Action<Configuration> implicitDependencyManagementCollector;

	private final Action<Configuration> dependencyManagementApplier;

	private final DependencyManagementContainer dependencyManagementContainer;

	/**
	 * Creates a new {@code InternalComponents} that will create and provide components
	 * for the given {@code project}.
	 * @param project the project
	 */
	public InternalComponents(Project project) {
		this.project = project;
		DependencyManagementConfigurationContainer configurationContainer = new DependencyManagementConfigurationContainer(
				project);
		MavenPomResolver pomResolver = new MavenPomResolver(project, configurationContainer);
		this.dependencyManagementContainer = new DependencyManagementContainer(project, pomResolver);
		DependencyManagementSettings dependencyManagementSettings = new DependencyManagementSettings();
		this.dependencyManagementExtension = new StandardDependencyManagementExtension(
				this.dependencyManagementContainer, configurationContainer, project, dependencyManagementSettings);
		this.implicitDependencyManagementCollector = new ImplicitDependencyManagementCollector(
				this.dependencyManagementContainer, dependencyManagementSettings);
		this.dependencyManagementApplier = new DependencyManagementApplier(project, this.dependencyManagementContainer,
				configurationContainer, dependencyManagementSettings, pomResolver);
	}

	/**
	 * Returns the {@link DependencyManagementExtension}.
	 * @return the extension
	 */
	public DependencyManagementExtension getDependencyManagementExtension() {
		return this.dependencyManagementExtension;
	}

	/**
	 * Returns the {@link Action} that can be applied to a {@link Configuration} to
	 * collect implicit dependency management from its dependencies.
	 * @return the action
	 */
	public Action<Configuration> getImplicitDependencyManagementCollector() {
		return this.implicitDependencyManagementCollector;
	}

	/**
	 * Returns the {@link Action} that can be applied to a {@link Configuration} to apply
	 * dependency management to its dependencies.
	 * @return the action
	 */
	public Action<Configuration> getDependencyManagementApplier() {
		return this.dependencyManagementApplier;
	}

	/**
	 * Creates a dependency management report task, assigning it the given
	 * {@code taskName}.
	 * @param taskName the task name
	 */
	public void createDependencyManagementReportTask(String taskName) {
		this.project.getTasks().create(taskName, DependencyManagementReportTask.class,
				(dependencyManagementReportTask) -> {
					dependencyManagementReportTask
							.setDependencyManagementContainer(InternalComponents.this.dependencyManagementContainer);
					dependencyManagementReportTask.setGroup(HelpTasksPlugin.HELP_GROUP);
					dependencyManagementReportTask.setDescription("Displays the dependency management declared in "
							+ dependencyManagementReportTask.getProject() + ".");
				});
	}

}
