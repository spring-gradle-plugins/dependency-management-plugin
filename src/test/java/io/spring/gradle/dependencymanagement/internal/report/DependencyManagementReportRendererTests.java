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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DependencyManagementReportRenderer}.
 *
 * @author Andy Wilkinson
 *
 */
class DependencyManagementReportRendererTests {

	private final StringWriter textOutput = new StringWriter();

	private final DependencyManagementReportRenderer renderer = new DependencyManagementReportRenderer(
			new PrintWriter(this.textOutput));

	@Test
	void projectHeaderForRootProject() {
		Project rootProject = ProjectBuilder.builder().build();
		this.renderer.startProject(rootProject);
		assertThat(outputLines()).containsExactly("", "------------------------------------------------------------",
				"Root project", "------------------------------------------------------------", "");
	}

	@Test
	void projectHeaderForSubproject() {
		Project subproject = ProjectBuilder.builder()
			.withParent(ProjectBuilder.builder().build())
			.withName("alpha")
			.build();
		this.renderer.startProject(subproject);
		assertThat(outputLines()).containsExactly("", "------------------------------------------------------------",
				"Project :alpha", "------------------------------------------------------------", "");
	}

	@Test
	void projectHeaderForSubprojectWithDescription() {
		Project subproject = ProjectBuilder.builder()
			.withParent(ProjectBuilder.builder().build())
			.withName("alpha")
			.build();
		subproject.setDescription("description of alpha project");
		this.renderer.startProject(subproject);
		assertThat(outputLines()).containsExactly("", "------------------------------------------------------------",
				"Project :alpha - description of alpha project",
				"------------------------------------------------------------", "");
	}

	@Test
	void globalDependencyManagementWithNoManagedVersions() {
		this.renderer.renderGlobalManagedVersions(Collections.emptyMap());
		assertThat(outputLines()).containsExactly("global - Default dependency management for all configurations",
				"No dependency management", "");
	}

	@Test
	void globalDependencyManagementWithManagedVersions() {
		Map<String, String> managedVersions = new HashMap<>();
		managedVersions.put("com.example:bravo", "1.0.0");
		managedVersions.put("com.example:alpha", "1.2.3");
		this.renderer.renderGlobalManagedVersions(managedVersions);
		assertThat(outputLines()).containsExactly("global - Default dependency management for all configurations",
				"	com.example:alpha 1.2.3", "	com.example:bravo 1.0.0", "");
	}

	@Test
	void configurationDependencyManagementWitNoManagedVersionsAtAll() {
		Configuration configuration = ProjectBuilder.builder().build().getConfigurations().create("test");
		this.renderer.renderConfigurationManagedVersions(Collections.emptyMap(), configuration, Collections.emptyMap());
		assertThat(outputLines()).containsExactly("test - Dependency management for the test configuration",
				"No dependency management", "");
	}

	@Test
	void configurationDependencyManagementWithOnlyGlobalManagedVersions() {
		Map<String, String> managedVersions = Collections.singletonMap("a:b", "1.0");
		Configuration configuration = ProjectBuilder.builder().build().getConfigurations().create("test");
		this.renderer.renderConfigurationManagedVersions(managedVersions, configuration, managedVersions);
		assertThat(outputLines()).containsExactly("test - Dependency management for the test configuration",
				"No configuration-specific dependency management", "");
	}

	@Test
	void configurationDependencyManagement() {
		Map<String, String> managedVersions = new HashMap<>();
		managedVersions.put("com.example:bravo", "1.0.0");
		managedVersions.put("com.example:alpha", "1.2.3");
		Configuration configuration = ProjectBuilder.builder().build().getConfigurations().create("test");
		this.renderer.renderConfigurationManagedVersions(managedVersions, configuration, Collections.emptyMap());
		assertThat(outputLines()).containsExactly("test - Dependency management for the test configuration",
				"	com.example:alpha 1.2.3", "	com.example:bravo 1.0.0", "");
	}

	private List<String> outputLines() {
		BufferedReader reader = new BufferedReader(new StringReader(this.textOutput.toString()));
		String line;
		List<String> lines = new ArrayList<>();
		try {
			while ((line = reader.readLine()) != null) {
				lines.add(line);
			}
		}
		catch (IOException ex) {
			throw new RuntimeException(ex);
		}
		return lines;
	}

}
