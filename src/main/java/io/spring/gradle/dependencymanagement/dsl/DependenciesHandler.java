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

import groovy.lang.Closure;

/**
 * A handler for configuring managed dependencies.
 *
 * @author Andy Wilkinson
 * @see DependencyManagementHandler#dependencies(Closure)
 */
public interface DependenciesHandler {

    /**
     * Configures dependency management for a set of dependencies with the same {@code group} and {@code version}.
     * Entries are added to the set using the given {@code closure} that is called with a {@link DependencySetHandler}
     * as its delegate.
     *
     * @param setSpecification a map containing the {@code group} and {@code version} of the set of dependencies
     * @param closure the closure that will configure the entries
     * @see DependencySetHandler
     */
    void dependencySet(Map<String, String> setSpecification, Closure closure);

    /**
     * Configures dependency management for the dependency identified by the given {@code id}. The id is a string of the
     * form {@code group:name:version}.
     *
     * @param id the id of the dependency
     */
    void dependency(Object id);

    /**
     * Configures dependency management for the dependency identified by the given {@code id} the dependency
     * management can be further configured using the given {@code closure} that is called with a
     * {@link DependencyHandler} as its delegate. The id is a string of the form {@code group:name:version}.
     *
     * @param id the id of the dependency
     * @param closure used to further configure the dependency management
     * @see DependencyHandler
     */
    void dependency(Object id, Closure closure);
}
