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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.spring.gradle.dependencymanagement.dsl.DependencyHandler;
import io.spring.gradle.dependencymanagement.internal.Exclusion;
import org.gradle.api.InvalidUserDataException;

/**
 * Standard implementation of {@link DependencyHandler}.
 *
 * @author Andy Wilkinson
 */
final class StandardDependencyHandler implements DependencyHandler {

	private final List<Exclusion> exclusions = new ArrayList<Exclusion>();

	@Override
	public void exclude(String exclusion) {
		String[] components = exclusion.split(":");
		if (components.length != 2) {
			throw new InvalidUserDataException(
					"Exclusion '" + exclusion + "' is malformed. The required" + " form is 'group:name'");
		}
		this.exclusions.add(new Exclusion(components[0], components[1]));
	}

	@Override
	public void exclude(Map<String, String> exclusion) {
		String group = exclusion.get("group");
		String name = exclusion.get("name");
		if (!hasText(group) || !hasText(name)) {
			throw new InvalidUserDataException("An exclusion requires both a group and a name");
		}
		this.exclusions.add(new Exclusion(group, name));
	}

	List<Exclusion> getExclusions() {
		return this.exclusions;
	}

	private boolean hasText(String string) {
		return string != null && string.trim().length() > 0;
	}

}
