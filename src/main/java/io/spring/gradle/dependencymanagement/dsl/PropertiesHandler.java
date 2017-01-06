/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
 * A handler for configuring and accessing properties management.
 *
 * @author Julien Lafourcade
 */
public interface PropertiesHandler {

    /**
     * Adds the given property in the form {@code name:value}.
     *
     * @param property the property
     */
    void property(String property);

    /**
     * Adds the given property using the {@code name} and {@code value} entries in the map.
     *
     * @param property the property
     */
    void property(Map<String, String> property);

}
