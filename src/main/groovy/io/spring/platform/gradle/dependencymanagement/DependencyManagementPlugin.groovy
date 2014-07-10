/*
 * Copyright 2014 the original author or authors.
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

package io.spring.platform.gradle.dependencymanagement

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails


class DependencyManagementPlugin implements Plugin<Project> {

	DependencyManagement versionManagement

	@Override
	public void apply(Project project) {
		Configuration configuration = project.getConfigurations().detachedConfiguration()

		versionManagement = new DependencyManagement(configuration: configuration, project: project)

		project.extensions.add("dependencyManagement", DependencyManagementExtension)
		project.extensions.configure(DependencyManagementExtension) {
			it.configuration = configuration
			it.dependencies = project.dependencies
		}

		project.configurations.all {
			resolutionStrategy {
				eachDependency { DependencyResolveDetails details ->
					versionManagement.apply(details)
				}
			}
		}
	}
}
