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

package io.spring.gradle.dependencymanagement.internal.dsl;

import java.util.List;
import java.util.function.Consumer;

import groovy.lang.Closure;
import io.spring.gradle.dependencymanagement.dsl.DependenciesHandler;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementConfigurer;
import io.spring.gradle.dependencymanagement.dsl.ImportsHandler;
import org.gradle.api.Action;

/**
 * A {@link DependencyManagementConfigurer} that delegates to one or more
 * {@code DependencyManagementConfigurers} allowing the dependency management for multiple
 * configurations to be configured simultaneously.
 *
 * @author Andy Wilkinson
 */
class CompoundDependencyManagementConfigurer implements DependencyManagementConfigurer {

	private final List<DependencyManagementConfigurer> delegates;

	CompoundDependencyManagementConfigurer(List<DependencyManagementConfigurer> delegates) {
		this.delegates = delegates;
	}

	@Override
	public void imports(Closure<?> closure) {
		doWithDelegates((delegate) -> delegate.imports(closure));
	}

	@Override
	public void imports(Action<ImportsHandler> action) {
		doWithDelegates((delegate) -> delegate.imports(action));
	}

	@Override
	public void dependencies(Closure<?> closure) {
		doWithDelegates((delegate) -> delegate.dependencies(closure));
	}

	@Override
	public void dependencies(Action<DependenciesHandler> action) {
		doWithDelegates((delegate) -> delegate.dependencies(action));
	}

	private void doWithDelegates(Consumer<DependencyManagementConfigurer> delegate) {
		this.delegates.forEach(delegate);
	}

}
