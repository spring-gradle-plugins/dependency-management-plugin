/*
 * Copyright 2014-2024 the original author or authors.
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

import java.io.PrintWriter;
import java.util.Map;
import java.util.TreeMap;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

/**
 * {@code DependencyManagementReportRenderer} renders a report the describes a
 * {@link Project Project's} dependency management.
 *
 * @author Andy Wilkinson.
 */
class DependencyManagementReportRenderer {

	private final PrintWriter output;

	DependencyManagementReportRenderer() {
		this(new PrintWriter(System.out));
	}

	DependencyManagementReportRenderer(PrintWriter writer) {
		this.output = writer;
	}

	void startProject(String path, String description, boolean root) {
		this.output.println();
		this.output.println("------------------------------------------------------------");
		String heading = root ? "Root project" : ("Project " + path);
		if (description != null) {
			heading += " - " + description;
		}
		this.output.println(heading);
		this.output.println("------------------------------------------------------------");
		this.output.println();
	}

	void renderGlobalManagedVersions(Map<String, String> globalManagedVersions) {
		renderDependencyManagementHeader("global", "Default dependency management for all configurations");
		if (globalManagedVersions != null && !globalManagedVersions.isEmpty()) {
			renderManagedVersions(globalManagedVersions);
		}
		else {
			this.output.println("No dependency management");
			this.output.println();
		}
		this.output.flush();
	}

	private void renderDependencyManagementHeader(String identifier, String description) {
		this.output.println(identifier + " - " + description);
	}

	private void renderManagedVersions(Map<String, String> managedVersions) {
		Map<String, String> sortedVersions = new TreeMap<>((one, two) -> {
			String[] oneComponents = one.split(":");
			String[] twoComponents = two.split(":");
			int result = oneComponents[0].compareTo(twoComponents[0]);
			if (result == 0) {
				result = oneComponents[1].compareTo(twoComponents[1]);
			}
			return result;
		});
		sortedVersions.putAll(managedVersions);
		sortedVersions.forEach((key, value) -> this.output.println("	" + key + " " + value));
		this.output.println();
	}

	void renderConfigurationManagedVersions(Map<String, String> managedVersions, Configuration configuration,
			Map<String, String> globalManagedVersions) {
		renderDependencyManagementHeader(configuration.getName(),
				"Dependency management for the " + configuration.getName() + " configuration");
		if (managedVersions != null && !managedVersions.isEmpty()) {
			if (!managedVersions.equals(globalManagedVersions)) {
				renderManagedVersions(managedVersions);
			}
			else {
				this.output.println("No configuration-specific dependency management");
				this.output.println();
			}
		}
		else {
			this.output.println("No dependency management");
			this.output.println();
		}
		this.output.flush();
	}

}
