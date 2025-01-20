/*
 * Copyright 2014-2024 the original author or authors.
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

import io.spring.gradle.dependencymanagement.dsl.GeneratedPomCustomizationHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings.PomCustomizationSettings;

/**
 * Standard implementation of {@link GeneratedPomCustomizationHandler}.
 *
 * @author Andy Wilkinson
 * @author Yanming Zhou
 */
class StandardGeneratedPomCustomizationHandler implements GeneratedPomCustomizationHandler {

	private final PomCustomizationSettings settings;

	StandardGeneratedPomCustomizationHandler(PomCustomizationSettings settings) {
		this.settings = settings;
	}

	@Override
	public boolean isEnabled() {
		return this.settings.isEnabled();
	}

	@Override
	public void setEnabled(boolean enabled) {
		this.settings.setEnabled(enabled);
	}

	@Override
	public void enabled(boolean enabled) {
		this.settings.setEnabled(enabled);
	}

}
