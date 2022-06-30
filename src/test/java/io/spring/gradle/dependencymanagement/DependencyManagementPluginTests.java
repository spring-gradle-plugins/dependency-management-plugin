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

package io.spring.gradle.dependencymanagement;

import java.io.File;

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DependencyManagementPlugin}.
 *
 * @author Andy Wilkinson
 */
class DependencyManagementPluginTests {

	@TempDir
	private File projectDir;

	private Project project;

	@BeforeEach
	void setUp() {
		this.project = ProjectBuilder.builder().withProjectDir(this.projectDir).build();
	}

	@Test
	void whenPluginIsAppliedThenDependencyManagementExtensionIsAdded() {
		this.project.getPlugins().apply(DependencyManagementPlugin.class);
		assertThat(this.project.getExtensions().findByType(DependencyManagementExtension.class)).isNotNull();
	}

	@Test
	void whenPluginIsAppliedThenPomConfigurerIsAvailable() {
		this.project.getPlugins().apply(DependencyManagementPlugin.class);
		assertThat(this.project.getExtensions().findByType(DependencyManagementExtension.class).getPomConfigurer())
				.isNotNull();
	}

	@Test
	void whenPluginIsAppliedThenDependencyManagementReportTaskIsAdded() {
		this.project.getPlugins().apply(DependencyManagementPlugin.class);
		assertThat(this.project.getTasks().findByName("dependencyManagement")).isNotNull();
	}

}
