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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.internal.properties.PropertySource;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuilder;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuilderFactory;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.DefaultModelBuildingRequest;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.FileModelSource;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingException;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingResult;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelCache;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelProblem;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.resolution.ModelResolver;
import org.gradle.api.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the effective {@link Model} for a Maven pom.
 *
 * @author Andy Wilkinson
 */
final class EffectiveModelBuilder {

	private static final Logger logger = LoggerFactory.getLogger(EffectiveModelBuilder.class);

	private final ModelResolver modelResolver;

	EffectiveModelBuilder(Project project, DependencyManagementConfigurationContainer configurationContainer,
			PlatformCategoryAttributeConfigurer attributeConfigurer) {
		this.modelResolver = new ConfigurationModelResolver(project, configurationContainer, attributeConfigurer);
	}

	List<Model> buildModels(List<ModelInput> inputs) {
		List<Model> models = new ArrayList<Model>();
		InMemoryModelCache cache = new InMemoryModelCache();
		for (ModelInput input : inputs) {
			models.add(buildModel(input, cache));
		}
		return models;
	}

	private Model buildModel(ModelInput input, InMemoryModelCache cache) {
		DefaultModelBuildingRequest request = new DefaultModelBuildingRequest();
		request.setSystemProperties(System.getProperties());
		request.setModelSource(new FileModelSource(input.pom));
		request.setModelResolver(this.modelResolver);
		request.setModelCache(cache);

		try {
			ModelBuildingResult result = createModelBuilder(input.properties).build(request);
			List<ModelProblem> errors = extractErrors(result.getProblems());
			if (!errors.isEmpty()) {
				reportErrors(errors, input.pom);
			}
			return result.getEffectiveModel();
		}
		catch (ModelBuildingException ex) {
			logger.debug("Model building failed", ex);
			reportErrors(extractErrors(ex.getProblems()), input.pom);
			return ex.getResult().getEffectiveModel();
		}
	}

	private List<ModelProblem> extractErrors(List<ModelProblem> problems) {
		List<ModelProblem> errors = new ArrayList<ModelProblem>();
		for (ModelProblem problem : problems) {
			if (problem.getSeverity() == ModelProblem.Severity.ERROR) {
				errors.add(problem);
			}
		}
		return errors;
	}

	private void reportErrors(List<ModelProblem> errors, File file) {
		StringBuilder message = new StringBuilder("Errors occurred while build effective model from " + file + ":");
		for (ModelProblem error : errors) {
			message.append("\n	" + error.getMessage() + " in " + error.getModelId());
		}
		logger.error(message.toString());
	}

	private DefaultModelBuilder createModelBuilder(PropertySource properties) {
		DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
		modelBuilder.setModelInterpolator(new PropertiesModelInterpolator(properties));
		modelBuilder.setModelValidator(new RelaxedModelValidator());
		return modelBuilder;
	}

	/**
	 * Input to a model building request.
	 */
	static class ModelInput {

		private final File pom;

		private final PropertySource properties;

		ModelInput(File pom, PropertySource properties) {
			this.pom = pom;
			this.properties = properties;
		}

	}

	private static final class InMemoryModelCache implements ModelCache {

		private final Map<Key, Object> cache = new ConcurrentHashMap<Key, Object>();

		@Override
		public Object get(String groupId, String artifactId, String version, String tag) {
			return this.cache.get(new Key(groupId, artifactId, version, tag));
		}

		@Override
		public void put(String groupId, String artifactId, String version, String tag, Object item) {
			this.cache.put(new Key(groupId, artifactId, version, tag), item);
		}

		private static final class Key {

			private final String groupId;

			private final String artifactId;

			private final String version;

			private final String tag;

			private Key(String groupId, String artifactId, String version, String tag) {
				this.groupId = groupId;
				this.artifactId = artifactId;
				this.version = version;
				this.tag = tag;
			}

			@Override
			public boolean equals(Object obj) {
				if (this == obj) {
					return true;
				}
				if ((obj == null) || (getClass() != obj.getClass())) {
					return false;
				}
				Key other = (Key) obj;
				if (!this.groupId.equals(other.groupId)) {
					return false;
				}
				if (!this.artifactId.equals(other.artifactId)) {
					return false;
				}
				if (!this.version.equals(other.version)) {
					return false;
				}
				if (!this.tag.equals(other.tag)) {
					return false;
				}
				return true;
			}

			@Override
			public int hashCode() {
				final int prime = 31;
				int result = 1;
				result = prime * result + this.groupId.hashCode();
				result = prime * result + this.artifactId.hashCode();
				result = prime * result + this.version.hashCode();
				result = prime * result + this.tag.hashCode();
				return result;
			}

		}

	}

}
