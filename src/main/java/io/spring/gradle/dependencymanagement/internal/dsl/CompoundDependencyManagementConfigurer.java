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

import java.util.List;

import groovy.lang.Closure;

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementHandler;

/**
 * A dependency management configurer that delegates to one or more {@link DependencyManagementHandler
 * DependencyManagementConfigurers}.
 *
 * @author Andy Wilkinson
 */
class CompoundDependencyManagementConfigurer implements DependencyManagementHandler {

    private final List<DependencyManagementHandler> delegates;

    CompoundDependencyManagementConfigurer(List<DependencyManagementHandler> delegates) {
        this.delegates = delegates;
    }

    @Override
    public void imports(Closure closure) {
        for (DependencyManagementHandler delegate: this.delegates) {
            delegate.imports(closure);
        }
    }

    @Override
    public void dependencies(final Closure closure) {
        for (DependencyManagementHandler delegate: this.delegates) {
            delegate.dependencies(closure);
        }
    }

}
