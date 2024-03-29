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

import groovy.lang.Closure;
import org.gradle.api.Action;

/**
 * An {@link Action} that's backed by a {@link Closure}.
 *
 * @param <T> the type handled by this action
 * @author Andy Wilkinson
 */
class ClosureBackedAction<T> implements Action<T> {

	private final Closure<?> closure;

	ClosureBackedAction(Closure<?> closure) {
		this.closure = closure;
	}

	@Override
	public void execute(T delegate) {
		this.closure.setResolveStrategy(Closure.DELEGATE_FIRST);
		this.closure.setDelegate(delegate);
		if (this.closure.getMaximumNumberOfParameters() == 0) {
			this.closure.call();
		}
		else {
			this.closure.call(delegate);
		}
	}

}
