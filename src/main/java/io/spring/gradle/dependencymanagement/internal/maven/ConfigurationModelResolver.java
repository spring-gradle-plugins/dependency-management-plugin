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

import java.util.HashMap;
import java.util.Map;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Repository;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.FileModelSource;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelSource;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.resolution.ModelResolver;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.resolution.UnresolvableModelException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

/**
 * A {@link ModelResolver} that uses a {@link Configuration} to resolve the
 * {@link ModelSource} for a pom. requested model.
 *
 * @author Andy Wilkinson
 */
class ConfigurationModelResolver implements ModelResolver {

	private final Map<String, FileModelSource> pomCache = new HashMap<String, FileModelSource>();

	private final Project project;

	private final DependencyManagementConfigurationContainer configurationContainer;

	private final PlatformCategoryAttributeConfigurer attributeConfigurer;

	ConfigurationModelResolver(Project project, DependencyManagementConfigurationContainer configurationContainer,
			PlatformCategoryAttributeConfigurer attributeConfigurer) {
		this.project = project;
		this.configurationContainer = configurationContainer;
		this.attributeConfigurer = attributeConfigurer;
	}

	@Override
	public ModelSource resolveModel(String groupId, String artifactId, String version)
			throws UnresolvableModelException {
		String coordinates = groupId + ":" + artifactId + ":" + version + "@pom";
		FileModelSource pom = this.pomCache.get(coordinates);
		if (pom == null) {
			pom = resolveModel(coordinates);
			this.pomCache.put(coordinates, pom);
		}
		return pom;
	}

	private FileModelSource resolveModel(final String coordinates) {
		Dependency dependency = this.project.getDependencies().create(coordinates);
		this.attributeConfigurer.configureCategoryAttribute(dependency);
		Configuration configuration = this.configurationContainer.newConfiguration(dependency);
		return new FileModelSource(configuration.resolve().iterator().next());
	}

	@Override
	public void addRepository(Repository repository) {
		// No-op. All repositories should be configured in the Gradle script.
	}

	@Override
	public ModelResolver newCopy() {
		return this;
	}

}
