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

package io.spring.gradle.dependencymanagement.internal.maven;

import java.lang.reflect.Method;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.util.GradleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for configuring the {@code org.gradle.category} attribute on a
 * {@link ModuleDependency} with a value of {@code platform}. The configuration is done
 * reflectively as the necessary APIs are not available in the version of Gradle against
 * which the code is compiled.
 * <p/>
 * Configuring the attribute works around a problem in Gradle 5 that prevents resolution
 * of a pom for which Gradle 5 has been used to publish Gradle module metadata. The
 * problem does not occur with Gradle 6 as it ignores metadata published by Gradle 5.
 *
 * @author Andy Wilkinson
 */
class PlatformCategoryAttributeConfigurer {

	private static final Logger logger = LoggerFactory.getLogger(PlatformCategoryAttributeConfigurer.class);

	void configureCategoryAttribute(Dependency dependency) {
		if (!(dependency instanceof ModuleDependency) || !isGradle5()) {
			return;
		}
		try {
			Method attributes = dependency.getClass().getMethod("attributes", Action.class);
			attributes.invoke(dependency, new Action<Object>() {

				@Override
				public void execute(Object container) {
					try {
						Class<?> attributeClass = Class.forName("org.gradle.api.attributes.Attribute");
						Object attribute = attributeClass.getMethod("of", String.class, Class.class).invoke(null,
								"org.gradle.category", String.class);
						Class.forName("org.gradle.api.attributes.AttributeContainer")
								.getMethod("attribute", attributeClass, Object.class)
								.invoke(container, attribute, "platform");
					}
					catch (Throwable ex) {
						logger.debug("Failed to configure platform attribute", ex);
					}
				}

			});
		}
		catch (Throwable ex) {
			logger.debug("Failed to configure platform attribute", ex);
		}
	}

	private boolean isGradle5() {
		return GradleVersion.current().getNextMajor().getVersion().startsWith("6.");
	}

}
