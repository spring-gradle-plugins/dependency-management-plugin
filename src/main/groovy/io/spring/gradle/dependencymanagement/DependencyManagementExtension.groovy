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

package io.spring.gradle.dependencymanagement

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class DependencyManagementExtension {

	protected DependencyManagementContainer dependencyManagementContainer

	protected Project project

    VersionHandler versions

    protected setDependencyManagementContainer(DependencyManagementContainer container) {
        this.dependencyManagementContainer = container;
        this.versions = new VersionHandler(container, null)
    }

	void imports(Closure closure) {
		new DependencyManagementHandler(dependencyManagementContainer).imports(closure)
	}

	void dependencies(Closure closure) {
		new DependencyManagementHandler(dependencyManagementContainer).dependencies(closure)
	}

    VersionHandler forConfiguration(String configurationName) {
        return new VersionHandler(dependencyManagementContainer, project.configurations.getByName(configurationName))
    }

    String getManagedVersion(String group, String name) {
        return new VersionHandler(dependencyManagementContainer, null).getManagedVersion(group, name)
    }

	def methodMissing(String name, args) {
		Closure closure
		if ("configurations" == name) {
			closure = args.last()
            def dependencyManagementContainer = this.dependencyManagementContainer
			List handlers = args.take(args.size() - 1).collect { Configuration configuration ->
				new DependencyManagementHandler(dependencyManagementContainer, configuration)
			}
			closure.delegate = new CompoundDependencyManagementHandler(handlers)
		} else {
			Configuration configuration = project.configurations.getAt(name)
			closure = args[0]
			closure.delegate = new DependencyManagementHandler(dependencyManagementContainer, configuration)
		}

		closure.resolveStrategy = Closure.DELEGATE_ONLY
		closure.call()
	}

	def propertyMissing(String name) {
		project.configurations.getAt(name)
	}

    private class VersionHandler {

        private final DependencyManagementContainer container

        private final Configuration configuration

        VersionHandler(DependencyManagementContainer container, Configuration configuration) {
            this.container = container
            this.configuration = configuration
        }

        String getManagedVersion(String group, String name) {
            container.getManagedVersion(configuration, group, name)
        }

        VersionHandler forConfiguration(String configurationName) {
            return new VersionHandler(this.container, project.configurations.getByName(configurationName))
        }
    }
}
