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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import groovy.lang.Closure;
import io.spring.gradle.dependencymanagement.dsl.DependenciesHandler;
import io.spring.gradle.dependencymanagement.dsl.DependencyHandler;
import io.spring.gradle.dependencymanagement.dsl.DependencySetHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

/**
 * Standard implementation of {@link DependenciesHandler}.
 *
 * @author Andy Wilkinson
 */
class StandardDependenciesHandler implements DependenciesHandler {

	private static final String KEY_GROUP = "group";

	private static final String KEY_NAME = "name";

	private static final String KEY_VERSION = "version";

	private final DependencyManagementContainer container;

	private final Configuration configuration;

	StandardDependenciesHandler(DependencyManagementContainer container, Configuration configuration) {
		this.container = container;
		this.configuration = configuration;
	}

	@Override
	public void dependency(String id) {
		dependency(id, (Action<DependencyHandler>) null);
	}

	@Override
	public void dependency(Map<String, String> id) {
		dependency(id, (Action<DependencyHandler>) null);
	}

	@Override
	public void dependency(String id, Closure<?> closure) {
		dependency(id, new ClosureBackedAction<>(closure));
	}

	@Override
	public void dependency(String id, Action<DependencyHandler> action) {
		String[] components = id.split(":");
		if (components.length != 3 || components[0].length() == 0 || components[1].length() == 0
				|| components[2].length() == 0) {
			throw new InvalidUserDataException(
					"Dependency identifier '" + id + "' is malformed. The required form is" + " 'group:name:version'");
		}
		configureDependency(components[0], components[1], components[2], action);
	}

	@Override
	public void dependency(Map<String, String> id, Closure<?> closure) {
		dependency(id, new ClosureBackedAction<>(closure));
	}

	@Override
	public void dependency(Map<String, String> id, Action<DependencyHandler> action) {
		Set<String> missingAttributes = new LinkedHashSet<>(Arrays.asList(KEY_GROUP, KEY_NAME, KEY_VERSION));
		missingAttributes.removeAll(id.keySet());
		if (!missingAttributes.isEmpty()) {
			throw new InvalidUserDataException(
					"Dependency identifier '" + id + "' did not specify " + toCommaSeparatedString(missingAttributes));
		}
		configureDependency(getAsString(id, KEY_GROUP), getAsString(id, KEY_NAME), getAsString(id, KEY_VERSION),
				action);
	}

	@Override
	public void dependencySet(String setId, Closure<?> closure) {
		dependencySet(setId, new ClosureBackedAction<>(closure));
	}

	@Override
	public void dependencySet(String setId, Action<DependencySetHandler> action) {
		String[] components = setId.split(":");
		if (components.length != 2) {
			throw new InvalidUserDataException("Dependency set identifier '" + setId + "' is malformed. The required "
					+ " form is 'group:version'");
		}
		configureDependencySet(components[0], components[1], action);
	}

	@Override
	public void dependencySet(Map<String, String> setSpecification, Closure<?> closure) {
		dependencySet(setSpecification, new ClosureBackedAction<>(closure));
	}

	@Override
	public void dependencySet(Map<String, String> setSpecification, Action<DependencySetHandler> action) {
		String group = getAsString(setSpecification, KEY_GROUP);
		String version = getAsString(setSpecification, KEY_VERSION);

		if (!hasText(group) || !hasText(version)) {
			throw new GradleException("A dependency set requires both a group and a version");
		}
		configureDependencySet(group, version, action);
	}

	private String getAsString(Map<? extends CharSequence, ? extends CharSequence> map, String key) {
		CharSequence charSequence = map.get(key);
		return (charSequence != null) ? charSequence.toString() : null;
	}

	private void configureDependencySet(String group, String version, Action<DependencySetHandler> action) {
		action.execute(new StandardDependencySetHandler(group.toString(), version.toString(), this.container,
				this.configuration));
	}

	private boolean hasText(String string) {
		return string != null && string.trim().length() > 0;
	}

	private String toCommaSeparatedString(Collection<String> items) {
		StringBuilder output = new StringBuilder();
		for (String item : items) {
			if (output.length() > 0) {
				output.append(", ");
			}
			output.append(item);
		}
		return output.toString();
	}

	private void configureDependency(String group, String name, String version, Action<DependencyHandler> action) {
		StandardDependencyHandler dependencyHandler = new StandardDependencyHandler();
		if (action != null) {
			action.execute(dependencyHandler);
		}
		this.container.addManagedVersion(this.configuration, group, name, version, dependencyHandler.getExclusions());
	}

	/**
	 * Handlers missing properties by returning the {@link Project} property with the
	 * given {@code name}.
	 * @param name the name of the property
	 * @return the value of the project property
	 */
	Object propertyMissing(String name) {
		return this.container.getProject().property(name);
	}

}
