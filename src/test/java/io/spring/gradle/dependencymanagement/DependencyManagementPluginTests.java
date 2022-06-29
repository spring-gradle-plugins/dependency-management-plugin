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

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DependencyManagementPlugin}.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementPluginTests {

	@Rule
	public TemporaryFolder temp = new TemporaryFolder();

	private Project project;

	@Before
	public void setUp() {
		this.project = ProjectBuilder.builder().withProjectDir(this.temp.getRoot()).build();
	}

	@Test
	public void whenPluginIsAppliedThenDependencyManagementExtensionIsAdded() {
		this.project.getPlugins().apply(DependencyManagementPlugin.class);
		assertThat(this.project.getExtensions().findByType(DependencyManagementExtension.class)).isNotNull();
	}

	@Test
	public void whenPluginIsAppliedThenPomConfigurerIsAvailable() {
		this.project.getPlugins().apply(DependencyManagementPlugin.class);
		assertThat(this.project.getExtensions().findByType(DependencyManagementExtension.class).getPomConfigurer())
				.isNotNull();
	}

	@Test
	public void whenPluginIsAppliedThenDependencyManagementReportTaskIsAdded() {
		this.project.getPlugins().apply(DependencyManagementPlugin.class);
		assertThat(this.project.getTasks().findByName("dependencyManagement")).isNotNull();
	}

}
