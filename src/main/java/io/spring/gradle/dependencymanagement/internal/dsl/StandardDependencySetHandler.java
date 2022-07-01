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

package io.spring.gradle.dependencymanagement.internal.dsl;

import groovy.lang.Closure;
import io.spring.gradle.dependencymanagement.dsl.DependencyHandler;
import io.spring.gradle.dependencymanagement.dsl.DependencySetHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;

/**
 * Standard implementation of {@link DependencySetHandler}.
 *
 * @author Andy Wilkinson
 */
final class StandardDependencySetHandler implements DependencySetHandler {

	private final String group;

	private final String version;

	private final DependencyManagementContainer dependencyManagementContainer;

	private final Configuration configuration;

	StandardDependencySetHandler(String group, String version,
			DependencyManagementContainer dependencyManagementContainer, Configuration configuration) {
		this.group = group;
		this.version = version;
		this.dependencyManagementContainer = dependencyManagementContainer;
		this.configuration = configuration;
	}

	@Override
	public void entry(String name) {
		entry(name, (Action<DependencyHandler>) null);
	}

	@Override
	public void entry(String name, final Closure<?> closure) {
		entry(name, new Action<DependencyHandler>() {

			@Override
			public void execute(DependencyHandler dependencyHandler) {
				if (closure != null) {
					closure.setDelegate(dependencyHandler);
					closure.call();
				}
			}
		});
	}

	@Override
	public void entry(String name, Action<DependencyHandler> action) {
		StandardDependencyHandler dependencyHandler = new StandardDependencyHandler();
		if (action != null) {
			action.execute(dependencyHandler);
		}
		this.dependencyManagementContainer.addManagedVersion(this.configuration, this.group, name, this.version,
				dependencyHandler.getExclusions());
	}

}
