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

import groovy.lang.Closure;
import org.gradle.api.Action;

/**
 * Handler for configuring dependency management imports.
 *
 * @author Andy Wilkinson
 * @see DependencyManagementHandler#imports(Closure)
 */
public interface ImportsHandler {

    /**
     * Imports the Maven bom with the given {@code coordinates} in the form {@code group:name:version}.
     *
     * @param coordinates the bom's coordinates
     */
    void mavenBom(String coordinates);

    /**
     * Imports the Maven bom with the given {@code coordinates} in the form {@code group:name:version}. The import
     * is customized using the given {@code closure} which is called with a {@link MavenBomHandler} as its delegate.
     *
     * @param coordinates the bom's coordinates
     * @param closure the closure
     * @see MavenBomHandler
     */
    void mavenBom(String coordinates, Closure closure);

    /**
     * Imports the Maven bom with the given {@code coordinates} in the form {@code group:name:version}. The import
     * is customized using the given {@code action}.
     *
     * @param coordinates the bom's coordinates
     * @param action the action
     */
    void mavenBom(String coordinates, Action<MavenBomHandler> action);

}
