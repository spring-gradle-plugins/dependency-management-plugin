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
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;

/**
 * A container for {@link Configuration Configurations} created by the dependency
 * management plugin that aren't part of the project's configurations.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementConfigurationContainer {

	private final List<Action<Configuration>> actions = new ArrayList<>();

	private final ConfigurationContainer delegate;

	/**
	 * Creates a new {@code DependencyManagementConfigurationContainer} that will manage
	 * {@link Configuration Configurations} for the given {@code project}.
	 * @param project the project
	 */
	public DependencyManagementConfigurationContainer(Project project) {
		this.delegate = project.getConfigurations();
	}

	/**
	 * Creates a new configuration with the given {@code dependencies}.
	 * @param dependencies the dependencies to add to the configuration
	 * @return the new configuration
	 */
	public Configuration newConfiguration(Dependency... dependencies) {
		return this.newConfiguration(null, dependencies);
	}

	/**
	 * Creates a new configuration and passes it to the given {@code configurer}. The
	 * given {@code dependencies} are added to the configuration.
	 * @param configurer the configurer
	 * @param dependencies the dependencies
	 * @return the new configuration
	 */
	Configuration newConfiguration(ConfigurationConfigurer configurer, Dependency... dependencies) {
		Configuration configuration = this.delegate.detachedConfiguration(dependencies);
		if (configurer != null) {
			configurer.configure(configuration);
		}
		for (Action<Configuration> action : this.actions) {
			action.execute(configuration);
		}
		return configuration;
	}

	/**
	 * Applies the given {@code action} to all of this container's {@link Configuration
	 * Configurations}.
	 * @param action the action to apply
	 */
	public void apply(Action<Configuration> action) {
		this.actions.add(action);
	}

	/**
	 * A callback capable of configuring a {@link Configuration}.
	 */
	@FunctionalInterface
	public interface ConfigurationConfigurer {

		/**
		 * Configure the given {@code configuration}.
		 * @param configuration the {@code Configuration} to configure
		 */
		void configure(Configuration configuration);

	}

}
