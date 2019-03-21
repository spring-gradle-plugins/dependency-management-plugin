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

package io.spring.gradle.dependencymanagement.internal.dsl;

import java.util.List;

import groovy.lang.Closure;
import org.gradle.api.Action;

import io.spring.gradle.dependencymanagement.dsl.DependenciesHandler;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementConfigurer;
import io.spring.gradle.dependencymanagement.dsl.ImportsHandler;

/**
 * A {@link DependencyManagementConfigurer} that delegates to one or more {@code DependencyManagementConfigurers}
 * allowing the dependency management for multiple configurations to be configured simultaneously.
 *
 * @author Andy Wilkinson
 */
class CompoundDependencyManagementConfigurer implements DependencyManagementConfigurer {

    private final List<DependencyManagementConfigurer> delegates;

    CompoundDependencyManagementConfigurer(List<DependencyManagementConfigurer> delegates) {
        this.delegates = delegates;
    }

    @Override
    public void imports(Closure closure) {
        for (DependencyManagementConfigurer delegate: this.delegates) {
            delegate.imports(closure);
        }
    }

    @Override
    public void imports(Action<ImportsHandler> action) {
        for (DependencyManagementConfigurer delegate: this.delegates) {
            delegate.imports(action);
        }
    }

    @Override
    public void dependencies(Closure closure) {
        for (DependencyManagementConfigurer delegate: this.delegates) {
            delegate.dependencies(closure);
        }
    }

    @Override
    public void dependencies(Action<DependenciesHandler> action) {
        for (DependencyManagementConfigurer delegate: this.delegates) {
            delegate.dependencies(action);
        }
    }

}
