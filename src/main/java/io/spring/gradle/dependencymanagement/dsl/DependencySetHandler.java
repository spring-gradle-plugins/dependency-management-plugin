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

package io.spring.gradle.dependencymanagement.dsl;

import groovy.lang.Closure;
import org.gradle.api.Action;

/**
 * A handler for the configuration of a set of dependencies with a common group and
 * version.
 *
 * @author Andy Wilkinson
 * @see DependenciesHandler#dependencySet(java.util.Map, Closure)
 */
public interface DependencySetHandler {

	/**
	 * Adds an entry to the set for the dependency with the given name.
	 * @param name the name of the dependency
	 */
	void entry(String name);

	/**
	 * Adds an entry to the set for the dependency with the given {@code name}. The
	 * dependency management for the entry is further configured using the given
	 * {@code closure} that is called with a {@link DependencyHandler} as its delegate.
	 * @param name the name of the dependency
	 * @param closure used to further configure the dependency management
	 * @see DependencyHandler
	 */
	void entry(String name, Closure closure);

	/**
	 * Adds an entry to the set for the dependency with the given {@code name}. The
	 * dependency management for the entry is further configured using the given
	 * {@code action}.
	 * @param name the name of the dependency
	 * @param action used to further configure the dependency management
	 */
	void entry(String name, Action<DependencyHandler> action);

}
