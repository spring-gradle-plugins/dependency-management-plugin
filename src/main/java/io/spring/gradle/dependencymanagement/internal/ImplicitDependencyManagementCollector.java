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

package io.spring.gradle.dependencymanagement.internal;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link Action} that adds an implict managed versions to the dependency management
 * for each of the {@link Configuration Configuration's} dependencies that has a version
 * that is not dynamic.
 *
 * @author Andy Wilkinson
 */
public class ImplicitDependencyManagementCollector implements Action<Configuration> {

	private static final Logger logger = LoggerFactory.getLogger(ImplicitDependencyManagementCollector.class);

	private final DependencyManagementContainer dependencyManagementContainer;

	private final DependencyManagementSettings dependencyManagementSettings;

	/**
	 * Creates a new {@code ImplicityDependencyManagementCollector} that will collect
	 * implicit dependency management in the given {@code dependencyManagementContainer}.
	 * @param dependencyManagementContainer the container
	 * @param dependencyManagementSettings the settings that control the collector's
	 * behavior
	 */
	public ImplicitDependencyManagementCollector(DependencyManagementContainer dependencyManagementContainer,
			DependencyManagementSettings dependencyManagementSettings) {
		this.dependencyManagementContainer = dependencyManagementContainer;
		this.dependencyManagementSettings = dependencyManagementSettings;
	}

	@Override
	public void execute(final Configuration root) {
		root.getIncoming().beforeResolve(new Action<ResolvableDependencies>() {

			@Override
			public void execute(ResolvableDependencies resolvableDependencies) {
				if (ImplicitDependencyManagementCollector.this.dependencyManagementSettings
						.isOverriddenByDependencies()) {
					for (Configuration configuration : root.getHierarchy()) {
						processConfiguration(configuration);
					}
				}
			}

		});
	}

	private void processConfiguration(Configuration configuration) {
		for (ModuleDependency dependency : getVersionedModuleDependencies(configuration)) {
			if (Versions.isDynamic(dependency.getVersion())) {
				logger.debug("Dependency '{}' in configuration '{}' has a dynamic version. The version will not be "
						+ " added to the managed versions", dependency, configuration.getName());
			}
			else {
				logger.debug("Adding managed version in configuration '{}' for dependency '{}'",
						configuration.getName(), dependency);
				this.dependencyManagementContainer.addImplicitManagedVersion(configuration, dependency.getGroup(),
						dependency.getName(), dependency.getVersion());
			}
		}
	}

	private List<ModuleDependency> getVersionedModuleDependencies(Configuration configuration) {
		List<ModuleDependency> versionedModuleDependencies = new ArrayList<ModuleDependency>();
		for (Dependency dependency : configuration.getIncoming().getDependencies()) {
			if (dependency instanceof ModuleDependency && dependency.getVersion() != null) {
				versionedModuleDependencies.add((ModuleDependency) dependency);
			}
		}
		return versionedModuleDependencies;
	}

}
