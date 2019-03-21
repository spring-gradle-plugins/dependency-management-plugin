/*
 * Copyright 2014-2016 the original author or authors.
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

import java.util.Map;

/**
 * A handler for configuring and accessing dependency management.
 *
 * @author Andy Wilkinson
 */
public interface DependencyManagementHandler extends DependencyManagementConfigurer {

    /**
     * Returns the properties from any imported boms.
     *
     * @return the imported properties
     */
    Map<String, String> getImportedProperties();

    /**
     * Returns a map of the managed versions for the configuration associated with this handler. The entire {@link
     * org.gradle.api.artifacts.Configuration#getHierarchy()} configuration hierarchy} is considered. The key-value
     * pairs in the map have the form {@code group:name = version}.
     *
     * @return the managed versions
     */
    Map<String, String> getManagedVersions();

}
