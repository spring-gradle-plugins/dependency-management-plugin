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

import org.gradle.api.artifacts.Configuration

/**
 * Internal handler for the dependency management DSL
 *
 * @author Andy Wilkinson
 */
class DependencyManagementHandler {

	private DependencyManagementContainer container

    private Configuration configuration

    DependencyManagementHandler(DependencyManagementContainer container) {
        this(container, null)
    }

	DependencyManagementHandler(DependencyManagementContainer container, Configuration configuration) {
		this.container = container
        this.configuration = configuration
	}

	void imports(Closure closure) {
		closure.setResolveStrategy(Closure.DELEGATE_FIRST)
		closure.delegate = new ImportsHandler(this.container, this.configuration)
		closure.call()
	}

	void dependencies(Closure closure) {
		closure.setResolveStrategy(Closure.DELEGATE_FIRST)
		closure.delegate = new DependenciesHandler(this.container, this.configuration)
		closure.call()
	}
}
