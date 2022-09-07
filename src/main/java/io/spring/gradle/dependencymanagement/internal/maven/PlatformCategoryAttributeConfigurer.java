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

import org.gradle.api.Action;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.attributes.Category;
import org.gradle.util.GradleVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support class for configuring the {@code org.gradle.category} attribute on a
 * {@link ModuleDependency} with a value of {@code platform}.
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
			((ModuleDependency) dependency).attributes(new Action<AttributeContainer>() {

				@Override
				public void execute(AttributeContainer container) {
					Attribute<String> attribute = Attribute.of("org.gradle.category", String.class);
					container.attribute(attribute, Category.REGULAR_PLATFORM);
				}

			});
		}
		catch (Throwable ex) {
			logger.debug("Failed to configure platform attribute", ex);
		}
	}

	private boolean isGradle5() {
		GradleVersion current = GradleVersion.current().getBaseVersion();
		return (current.compareTo(GradleVersion.version("5.0")) >= 0)
				&& (current.compareTo(GradleVersion.version("6.0")) < 0);
	}

}
