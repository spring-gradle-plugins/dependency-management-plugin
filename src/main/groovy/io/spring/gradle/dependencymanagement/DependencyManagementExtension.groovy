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

public class DependencyManagementExtension {

	DependencyManagementContainer dependencyManagementContainer

	Project project

	void imports(Closure closure) {
		new DependencyManagementHandler(dependencyManagementContainer.globalDependencyManagement).imports(closure)
	}

	void dependencies(Closure closure) {
		new DependencyManagementHandler(dependencyManagementContainer.globalDependencyManagement).dependencies(closure)
	}

	def methodMissing(String name, args) {
		Closure closure
		if ("configurations" == name) {
			closure = args.last()
			def handlers = args.take(args.size() - 1).collect { configuration ->
				new DependencyManagementHandler(dependencyManagementContainer.dependencyManagementForConfiguration(configuration))
			}
			closure.delegate = new CompoundDependencyManagementHandler(handlers)
		} else {
			Configuration configuration = project.configurations.getAt(name)
			closure = args[0]
			closure.delegate = new DependencyManagementHandler(dependencyManagementContainer.dependencyManagementForConfiguration(configuration))
		}

		closure.resolveStrategy = Closure.DELEGATE_ONLY
		closure.call()
	}

	def propertyMissing(String name) {
		project.configurations.getAt(name)
	}
}
