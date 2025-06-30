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

package io.spring.gradle.dependencymanagement.dsl;

/**
 * A handler for configuring the customization of generated POMs.
 *
 * @author Andy Wilkinson
 * @author Yanming Zhou
 */
public interface GeneratedPomCustomizationHandler {

	/**
	 * Whether or not customization of generated poms is enabled. Defaults to
	 * {@code true}.
	 * @return whether or not customization is enabled
	 */
	boolean isEnabled();

	/**
	 * Sets whether or not customization of generated poms is enabled. Defaults to
	 * {@code true}.
	 * @param enabled whether or not customization is enabled
	 */
	void setEnabled(boolean enabled);

	/**
	 * Sets whether or not customization of generated poms is enabled. Defaults to
	 * {@code true}.
	 * @param enabled whether or not customization is enabled
	 */
	void enabled(boolean enabled);

}
