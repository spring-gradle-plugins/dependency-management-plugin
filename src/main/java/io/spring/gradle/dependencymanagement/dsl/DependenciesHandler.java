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
import org.gradle.api.Action;

/**
 * A handler for configuring managed dependencies.
 *
 * @author Andy Wilkinson
 * @see DependencyManagementHandler#dependencies(Closure)
 * @see DependencyManagementHandler#dependencies(Action)
 */
public interface DependenciesHandler {

    /**
     * Configures dependency management for the dependency identified by the given {@code id}. The id is a string
     * of the form {@code group:name:version}.
     *
     * @param id the id of the dependency
     */
    void dependency(String id);

    /**
     * Configures dependency management for the dependency identified by the given {@code id}. The id is a map with
     * {@code group}, {@code name}, and {@code version} entries.
     *
     * @param id the id of the dependency
     */
    void dependency(Map<String, String> id);

    /**
     * Configures dependency management for the dependency identified by the given {@code id}. The dependency
     * management can be further configured using the given {@code closure} that is called with a {@link
     * DependencyHandler} as its delegate. The id is a string of the form {@code group:name:version}.
     *
     * @param id the id of the dependency
     * @param closure used to further configure the dependency management
     * @see DependencyHandler
     */
    void dependency(String id, Closure closure);

    /**
     * Configures dependency management for the dependency identified by the given {@code id}. The dependency
     * management can be further configured using the given {@code closure} that is called with a {@link
     * DependencyHandler} as its delegate. The id is a map with {@code group}, {@code name}, and {@code version}
     * entries.
     *
     * @param id the id of the dependency
     * @param closure used to further configure the dependency management
     * @see DependencyHandler
     */
    void dependency(Map<String, String> id, Closure closure);

    /**
     * Configures dependency management for the dependency identified by the given {@code id}. The dependency
     * management can be further configured using the given {@code action}. The id is a string of the form
     * {@code group:name:version} or a map with {@code group}, {@code name}, and {@code version} entries.
     *
     * @param id the id of the dependency
     * @param action used to further configure the dependency management
     * @see DependencyHandler
     */
    void dependency(String id, Action<DependencyHandler> action);

    /**
     * Configures dependency management for the dependency identified by the given {@code id}. The dependency
     * management can be further configured using the given {@code action}. The id is a map with {@code group},
     * {@code name}, and {@code version} entries.
     *
     * @param id the id of the dependency
     * @param action used to further configure the dependency management
     * @see DependencyHandler
     */
    void dependency(Map<String, String> id, Action<DependencyHandler> action);

    /**
     * Configures dependency management for a set of dependencies with the same {@code group} and {@code version}
     * as specified by the given {@code setId}. The id is a string of the form {@code group:version}. Entries are added
     * to the set using the given {@code closure} that is called with a {@link DependencySetHandler} as its delegate.
     *
     * @param setId the id ({@code group:version}) of the set
     * @param closure used to configure the entries
     * @see DependencySetHandler
     */
    void dependencySet(String setId, Closure closure);

    /**
     * Configures dependency management for a set of dependencies with the same {@code group} and {@code version}
     * as specified by the given {@code setId}. The id is a string of the form {@code group:version}. Entries are added
     * to the set using the given {@code action}.
     *
     * @param setId the id ({@code group:version}) of the set
     * @param action used to configure the entries
     * @see DependencySetHandler
     */
    void dependencySet(String setId, Action<DependencySetHandler> action);

    /**
     * Configures dependency management for a set of dependencies with the same {@code group} and {@code version} as
     * specified by the given {@code setId}. The id is a map with {@code group} and {@code version} entries. Entries
     * are added to the set using the given {@code closure} that is called with a {@link DependencySetHandler} as its
     * delegate.
     *
     * @param setId a map containing the {@code group} and {@code version} of the set of dependencies
     * @param closure used to configure the entries
     * @see DependencySetHandler
     */
    void dependencySet(Map<String, String> setId, Closure closure);

    /**
     * Configures dependency management for a set of dependencies with the same {@code group} and {@code version} as
     * specified by the given {@code setId}. The id is a map with {@code group} and {@code version} entries. Entries
     * are added to the set using the given {@code action}.
     *
     * @param setId a map containing the {@code group} and {@code version} of the set of dependencies
     * @param action used to configure the entries
     * @see DependencySetHandler
     */
    void dependencySet(Map<String, String> setId, Action<DependencySetHandler> action);

}
