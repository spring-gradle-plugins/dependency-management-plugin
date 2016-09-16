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

package io.spring.gradle.dependencymanagement.internal.dsl;

import groovy.lang.Closure;
import org.gradle.api.artifacts.Configuration;

import io.spring.gradle.dependencymanagement.dsl.DependencySetHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;

/**
 * Standard implementation of {@link DependencySetHandler}.
 *
 * @author Andy Wilkinson
 */
final class StandardDependencySetHandler implements DependencySetHandler {

    private final String group;

    private final String version;

    private final DependencyManagementContainer dependencyManagementContainer;

    private final Configuration configuration;

    StandardDependencySetHandler(String group, String version,
            DependencyManagementContainer dependencyManagementContainer, Configuration configuration) {
        this.group = group;
        this.version = version;
        this.dependencyManagementContainer = dependencyManagementContainer;
        this.configuration = configuration;
    }

    @Override
    public void entry(String name) {
        entry(name, null);
    }

    @Override
    public void entry(String name, Closure closure) {
        StandardDependencyHandler dependencyHandler = new StandardDependencyHandler();
        if (closure != null) {
            closure.setDelegate(dependencyHandler);
            closure.call();
        }
        this.dependencyManagementContainer.addManagedVersion(this.configuration, this.group, name, this.version,
                dependencyHandler.getExclusions());
    }

}
