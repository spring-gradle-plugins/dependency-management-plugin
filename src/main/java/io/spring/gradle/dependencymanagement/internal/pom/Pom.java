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

package io.spring.gradle.dependencymanagement.internal.pom;

import java.util.List;
import java.util.Map;

/**
 * A minimal representation of a Maven pom.
 *
 * @author Andy Wilkinson
 */
public class Pom {

	private final Coordinates coordinates;

	private final List<Dependency> managedDependencies;

	private final List<Dependency> dependencies;

	private final Map<String, String> properties;

	/**
	 * Creates a new pom.
	 * @param coordinates the coordinates of the pom
	 * @param managedDependencies the managed dependencies
	 * @param dependencies the dependencies
	 * @param properties the properties
	 */
	public Pom(Coordinates coordinates, List<Dependency> managedDependencies, List<Dependency> dependencies,
			Map<String, String> properties) {
		this.coordinates = coordinates;
		this.managedDependencies = managedDependencies;
		this.dependencies = dependencies;
		this.properties = properties;
	}

	/**
	 * Returns the pom's managed dependencies.
	 * @return the managed dependencies
	 */
	public List<Dependency> getManagedDependencies() {
		return this.managedDependencies;
	}

	/**
	 * Returns the pom's properties.
	 * @return the properties
	 */
	public Map<String, String> getProperties() {
		return this.properties;
	}

	/**
	 * Returns the pom's coordinates.
	 * @return the coordinates
	 */
	public Coordinates getCoordinates() {
		return this.coordinates;
	}

	/**
	 * Returns the pom's dependencies.
	 * @return the dependencies
	 */
	public List<Dependency> getDependencies() {
		return this.dependencies;
	}

}
