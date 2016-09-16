/*
 * Copyright 2014-2016 the original author or authors.
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
 * A handler for configuring a managed dependency.
 *
 * @author Andy Wilkinson
 * @see DependencySetHandler#entry(String, groovy.lang.Closure)
 * @see DependenciesHandler#dependency(Object, groovy.lang.Closure)
 */
public interface DependencyHandler {

    /**
     * Adds the given exclusion in the form {@code group:name}.
     *
     * @param exclusion the exclusion
     */
    void exclude(String exclusion);

    /**
     * Adds the given exclusion using the {@code group} and {@code name} entries in the map.
     *
     * @param exclusion the exclusion
     */
    void exclude(Map<String, Object> exclusion);

}
