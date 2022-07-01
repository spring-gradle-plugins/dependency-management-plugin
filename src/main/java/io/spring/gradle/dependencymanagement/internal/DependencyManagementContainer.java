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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;
import io.spring.gradle.dependencymanagement.internal.pom.PomResolver;
import io.spring.gradle.dependencymanagement.internal.properties.PropertySource;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Container object for a Gradle build project's dependency management, handling the
 * project's global and configuration-specific dependency management.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementContainer {

	private static final Logger logger = LoggerFactory.getLogger(DependencyManagementContainer.class);

	private final DependencyManagement globalDependencyManagement;

	private final PomResolver pomResolver;

	private final Project project;

	private final Map<Configuration, DependencyManagement> configurationDependencyManagement = new LinkedHashMap<>();

	/**
	 * Creates a new {@code DependencyManagementContainer} that will hold dependency
	 * management for the given {@code
	 * project}. The given {@code pomResolver} will be use to resolve any Maven poms.
	 * @param project the project
	 * @param pomResolver the pom resolver
	 */
	public DependencyManagementContainer(Project project, PomResolver pomResolver) {
		this.project = project;
		this.pomResolver = pomResolver;
		this.globalDependencyManagement = new DependencyManagement(this.project, this.pomResolver);
	}

	/**
	 * Returns the project associated with this dependency management container.
	 * @return the project
	 */
	public Project getProject() {
		return this.project;
	}

	void addImplicitManagedVersion(Configuration configuration, String group, String name, String version) {
		dependencyManagementForConfiguration(configuration).addImplicitManagedVersion(group, name, version);
	}

	/**
	 * Adds a managed version to the dependency management for the given
	 * {@code configuration}. The managed version is for the dependency with the given
	 * {@code group}, {@code name}, and {@code version} and has the given {@code
	 * exclusions}.
	 * @param configuration the configuration
	 * @param group the dependency's group
	 * @param name the dependency's name
	 * @param version the dependency's version
	 * @param exclusions the dependency's exclusions
	 */
	public void addManagedVersion(Configuration configuration, String group, String name, String version,
			List<Exclusion> exclusions) {
		dependencyManagementForConfiguration(configuration).addExplicitManagedVersion(group, name, version, exclusions);
	}

	/**
	 * Adds an import of a bom to the dependency management for the given
	 * {@code configuration}. The bom is identified by the given {@code coordinates} and
	 * the given {@code properties} will be used when resolving the bom's contents.
	 * @param configuration the configuration
	 * @param coordinates the coordinates of the bom
	 * @param properties the properties to use when resolving the bom's contents
	 */
	public void importBom(Configuration configuration, Coordinates coordinates, PropertySource properties) {
		dependencyManagementForConfiguration(configuration).importBom(coordinates, properties);
	}

	String getManagedVersion(Configuration configuration, String group, String name) {
		String version = null;
		if (configuration != null) {
			version = findManagedVersion(configuration, group, name);
		}
		if (version == null) {
			version = this.globalDependencyManagement.getManagedVersion(group, name);
			if (version != null) {
				logger.debug("Found managed version '{}' for dependency '{}:{}' in global dependency " + "management",
						version, group, name);
			}
		}
		return version;
	}

	private String findManagedVersion(Configuration source, String group, String name) {
		for (Configuration configuration : source.getHierarchy()) {
			String managedVersion = dependencyManagementForConfiguration(configuration).getManagedVersion(group, name);
			if (managedVersion != null) {
				logger.debug("Found managed version '{}' for dependency '{}:{}' in dependency management for "
						+ "configuration '{}'", managedVersion, group, name, configuration.getName());
				return managedVersion;
			}
		}
		return null;
	}

	/**
	 * Returns the {@link Exclusions} that have been configured for the given
	 * {@code configuration}.
	 * @param configuration the configuration
	 * @return the exclusions
	 */
	public Exclusions getExclusions(Configuration configuration) {
		Exclusions exclusions = new Exclusions();
		if (configuration != null) {
			for (Configuration inHierarchy : configuration.getHierarchy()) {
				exclusions.addAll(dependencyManagementForConfiguration(inHierarchy).getExclusions());
			}

		}
		exclusions.addAll(this.globalDependencyManagement.getExclusions());
		return exclusions;
	}

	/**
	 * Returns the properties from boms imported in the given {@code configuration}.
	 * @param configuration the configuration
	 * @return the properties
	 */
	public Map<String, String> importedPropertiesForConfiguration(Configuration configuration) {
		Map<String, String> properties = new HashMap<>();
		properties.putAll(this.globalDependencyManagement.getImportedProperties());
		if (configuration != null) {
			for (Configuration inHierarchy : getReversedHierarchy(configuration)) {
				properties.putAll(dependencyManagementForConfiguration(inHierarchy).getImportedProperties());
			}

		}
		return properties;
	}

	/**
	 * Returns the managed versions for the given {@code configuration} and its hierarchy.
	 * The returned map contains keys of the form {@code groupId:artifactId}.
	 * @param configuration the configuration, or {@code null} for managed versions in
	 * global dependency management
	 * @return the managed versions for the configuration
	 */
	public Map<String, String> getManagedVersionsForConfiguration(Configuration configuration) {
		return getManagedVersionsForConfiguration(configuration, true);
	}

	/**
	 * Returns the managed versions for the given {@code configuration}. The returned map
	 * contains keys of the form {@code groupId:artifactId}. If {@code inherited} is true,
	 * managed versions for the entire {@link Configuration#getHierarchy() configuration
	 * hierarchy} are returned.
	 * @param configuration the configuration, or {@code null} for managed versions in
	 * global dependency management
	 * @param inherited true if managed versions inherited from the configuration
	 * hierarchy should be returned
	 * @return the managed versions for the configuration
	 */
	public Map<String, String> getManagedVersionsForConfiguration(Configuration configuration, boolean inherited) {
		if (inherited) {
			Map<String, String> managedVersions = dependencyManagementForConfiguration(null).getManagedVersions();
			if (configuration != null) {
				for (Configuration inHierarchy : getReversedHierarchy(configuration)) {
					managedVersions.putAll(dependencyManagementForConfiguration(inHierarchy).getManagedVersions());
				}
			}
			return managedVersions;
		}
		return dependencyManagementForConfiguration(configuration).getManagedVersions();
	}

	private List<Configuration> getReversedHierarchy(Configuration configuration) {
		List<Configuration> hierarchy = new ArrayList<>(configuration.getHierarchy());
		Collections.reverse(hierarchy);
		return hierarchy;
	}

	private DependencyManagement dependencyManagementForConfiguration(Configuration configuration) {
		if (configuration == null) {
			return this.globalDependencyManagement;
		}
		else {
			DependencyManagement dependencyManagement = this.configurationDependencyManagement.get(configuration);
			if (dependencyManagement == null) {
				dependencyManagement = new DependencyManagement(this.project, configuration, this.pomResolver);
				this.configurationDependencyManagement.put(configuration, dependencyManagement);
			}
			return dependencyManagement;
		}

	}

	/**
	 * Returns this container's global {@link DependencyManagement}.
	 * @return the global dependency management
	 */
	public DependencyManagement getGlobalDependencyManagement() {
		return this.globalDependencyManagement;
	}

}
