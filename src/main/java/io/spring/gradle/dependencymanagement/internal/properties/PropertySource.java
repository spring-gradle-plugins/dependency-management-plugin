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

package io.spring.gradle.dependencymanagement.internal.properties;

/**
 * A source for properties.
 *
 * @author Andy Wilkinson
 */
@FunctionalInterface
public interface PropertySource {

	/**
	 * Returns the property with the given {@code name} or {@code null} if the source has
	 * no such property.
	 * @param name name of the property
	 * @return the property or {@code null}
	 */
	Object getProperty(String name);

}
