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

package io.spring.gradle.dependencymanagement.internal.report;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import org.gradle.api.DefaultTask;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.TaskAction;

/**
 * Task to display the dependency management for a project.
 *
 * @author Andy Wilkinson.
 */
public class DependencyManagementReportTask extends DefaultTask {

	private DependencyManagementContainer dependencyManagementContainer;

	private DependencyManagementReportRenderer renderer = new DependencyManagementReportRenderer();

	void setRenderer(DependencyManagementReportRenderer renderer) {
		this.renderer = renderer;
	}

	/**
	 * Sets the container for the dependency management that will be reported.
	 * @param dependencyManagementContainer the container
	 */
	public void setDependencyManagementContainer(DependencyManagementContainer dependencyManagementContainer) {
		this.dependencyManagementContainer = dependencyManagementContainer;
	}

	/**
	 * {@link TaskAction} that produces the dependency management report.
	 */
	@TaskAction
	public void report() {
		this.renderer.startProject(getProject());
		Map<String, String> globalManagedVersions = this.dependencyManagementContainer
			.getManagedVersionsForConfiguration(null);
		this.renderer.renderGlobalManagedVersions(globalManagedVersions);
		Set<Configuration> configurations = new TreeSet<>(Comparator.comparing(Configuration::getName));
		configurations.addAll(getProject().getConfigurations());
		for (Configuration configuration : configurations) {
			Map<String, String> managedVersions = this.dependencyManagementContainer
				.getManagedVersionsForConfiguration(configuration);
			this.renderer.renderConfigurationManagedVersions(managedVersions, configuration, globalManagedVersions);
		}
	}

}
