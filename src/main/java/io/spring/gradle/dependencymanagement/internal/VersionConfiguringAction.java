/*
 * Copyright 2014-2023 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link Action} to be applied to {@link DependencyResolveDetails} that configures the
 * dependency's version based on the dependency management.
 *
 * @author Andy Wilkinson
 */
class VersionConfiguringAction implements Action<DependencyResolveDetails> {

	private static final Logger logger = LoggerFactory.getLogger(VersionConfiguringAction.class);

	private final Project project;

	private final DependencyManagementContainer dependencyManagementContainer;

	private final Configuration configuration;

	private final LocalProjects localProjects;

	private Set<String> directDependencies;

	VersionConfiguringAction(Project project, DependencyManagementContainer dependencyManagementContainer,
			Configuration configuration) {
		this.project = project;
		this.localProjects = project.getGradle().getStartParameter().isConfigureOnDemand()
				? new StandardLocalProjects(project) : new CachingLocalProjects(project);
		this.dependencyManagementContainer = dependencyManagementContainer;
		this.configuration = configuration;
	}

	@Override
	public void execute(DependencyResolveDetails details) {
		logger.debug("Processing dependency '{}'", details.getRequested());
		if (isDependencyOnLocalProject(this.project, details)) {
			logger.debug("'{}' is a local project dependency. Dependency management has not been applied",
					details.getRequested());
			return;
		}
		if (isDirectDependency(details) && Versions.isDynamic(details.getRequested().getVersion())) {
			logger.debug("'{}' is a direct dependency and has a dynamic version. "
					+ "Dependency management has not been applied", details.getRequested());
			return;
		}
		String version = this.dependencyManagementContainer.getManagedVersion(this.configuration,
				details.getRequested().getGroup(), details.getRequested().getName());
		if (version != null) {
			logger.debug("Using version '{}' for dependency '{}'", version, details.getRequested());
			details.useVersion(version);
			return;
		}
		logger.debug("No dependency management for dependency '{}'", details.getRequested());
	}

	private boolean isDirectDependency(DependencyResolveDetails details) {
		if (this.directDependencies == null) {
			Set<String> directDependencies = new HashSet<>();
			for (Dependency dependency : this.configuration.getAllDependencies()) {
				directDependencies.add(dependency.getGroup() + ":" + dependency.getName());
			}
			this.directDependencies = directDependencies;
		}
		return this.directDependencies.contains(getId(details));
	}

	private boolean isDependencyOnLocalProject(Project project, DependencyResolveDetails details) {
		return this.localProjects.getNames().contains(getId(details));
	}

	private String getId(DependencyResolveDetails details) {
		return details.getRequested().getGroup() + ":" + details.getRequested().getName();
	}

	ResolutionStrategy applyTo(Configuration c) {
		return c.getResolutionStrategy().eachDependency(this);
	}

	private interface LocalProjects {

		Set<String> getNames();

	}

	private static final class StandardLocalProjects implements LocalProjects {

		private final Project project;

		private StandardLocalProjects(Project project) {
			this.project = project;
		}

		@Override
		public Set<String> getNames() {
			Set<String> names = new HashSet<>();
			for (Project localProject : this.project.getRootProject().getAllprojects()) {
				names.add(localProject.getGroup() + ":" + localProject.getName());
			}
			return names;
		}

	}

	private static final class CachingLocalProjects implements LocalProjects {

		private final LocalProjects delegate;

		private Set<String> localProjectNames;

		private CachingLocalProjects(Project project) {
			this.delegate = new StandardLocalProjects(project);
		}

		@Override
		public Set<String> getNames() {
			Set<String> names = this.localProjectNames;
			if (names == null) {
				names = this.delegate.getNames();
				this.localProjectNames = names;
			}
			return names;
		}

	}

}
