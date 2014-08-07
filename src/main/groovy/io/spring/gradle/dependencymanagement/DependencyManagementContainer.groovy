/*
 * Copyright 2012-2014 the original author or authors.
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

package io.spring.gradle.dependencymanagement;

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class DependencyManagementContainer {

	private final DependencyManagement globalDependencyManagement

	private final Project project

	private final Map<Configuration, DependencyManagement> configurationDependencyManagement = [:]

	DependencyManagementContainer(Project project) {
		this.globalDependencyManagement = new DependencyManagement(project)
		this.project = project
	}

    void addManagedVersion(Configuration configuration, String group, String name, String version) {
        dependencyManagementForConfiguration(configuration).addManagedVersion(group, name, version)
    }

    void importBom(Configuration configuration, String coordinates) {
        dependencyManagementForConfiguration(configuration).importBom(coordinates)
    }

    String getManagedVersion(Configuration configuration, String group, String name) {
        String version = null
        if (configuration) {
            version = configuration.hierarchy.findResult { dependencyManagementForConfiguration(it).getManagedVersion(group, name)}
        }
        if (version == null) {
            version = globalDependencyManagement.getManagedVersion(group, name)
        }
        version
    }

    private DependencyManagement dependencyManagementForConfiguration(Configuration configuration) {
        if (!configuration) {
            globalDependencyManagement
        } else {
            configurationDependencyManagement.get(configuration, new DependencyManagement(project))
        }
    }
}
