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

import java.util.Map;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import io.spring.gradle.dependencymanagement.internal.maven.MavenPomResolver;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DependencyManagementReportTask}.
 *
 * @author Andy Wilkinson
 */
class DependencyManagementReportTaskTests {

	private final Project project = ProjectBuilder.builder().build();

	private final DependencyManagementReportTask task = this.project.getTasks()
		.create("dependencyManagement", DependencyManagementReportTask.class);

	private final DependencyManagementReportRenderer renderer = mock(DependencyManagementReportRenderer.class);

	DependencyManagementReportTaskTests() {
		this.task.setDependencyManagementContainer(new DependencyManagementContainer(this.project,
				new MavenPomResolver(this.project, new DependencyManagementConfigurationContainer(this.project))));
		this.task.setRenderer(this.renderer);
	}

	@Test
	@SuppressWarnings("unchecked")
	void basicReport() {
		this.task.report();
		then(this.renderer).should().startProject(this.project.getPath(), this.project.getDescription(), true);
		then(this.renderer).should().renderGlobalManagedVersions(any(Map.class));
		then(this.renderer).shouldHaveNoMoreInteractions();
	}

	@Test
	@SuppressWarnings("unchecked")
	void reportForProjectWithConfigurations() {
		Configuration configurationOne = this.project.getConfigurations().create("second");
		Configuration configurationTwo = this.project.getConfigurations().create("first");
		this.task.report();
		then(this.renderer).should().startProject(this.project.getPath(), this.project.getDescription(), true);
		then(this.renderer).should().renderGlobalManagedVersions(any(Map.class));
		then(this.renderer).should()
			.renderConfigurationManagedVersions(any(Map.class), eq(configurationTwo), any(Map.class));
		then(this.renderer).should()
			.renderConfigurationManagedVersions(any(Map.class), eq(configurationOne), any(Map.class));
		then(this.renderer).shouldHaveNoMoreInteractions();
	}

}
