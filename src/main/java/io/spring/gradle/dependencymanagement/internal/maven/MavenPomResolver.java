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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.internal.Exclusion;
import io.spring.gradle.dependencymanagement.internal.maven.EffectiveModelBuilder.ModelInput;
import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;
import io.spring.gradle.dependencymanagement.internal.pom.Dependency;
import io.spring.gradle.dependencymanagement.internal.pom.Pom;
import io.spring.gradle.dependencymanagement.internal.pom.PomReference;
import io.spring.gradle.dependencymanagement.internal.pom.PomResolver;
import io.spring.gradle.dependencymanagement.internal.properties.CompositePropertySource;
import io.spring.gradle.dependencymanagement.internal.properties.MapPropertySource;
import io.spring.gradle.dependencymanagement.internal.properties.PropertySource;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.specs.Specs;

/**
 * A {@link PomResolver} that uses the jarjared Maven API to access the pom's model.
 *
 * @author Andy Wilkinson
 */
public class MavenPomResolver implements PomResolver {

	private final DependencyManagementConfigurationContainer configurationContainer;

	private final EffectiveModelBuilder effectiveModelBuilder;

	private final DependencyHandler dependencyHandler;

	/**
	 * Creates a new {@code MavenPomResolver}. Properties from the given {@code project}
	 * will be used during resolution. The given {@code configurationContainer} will be
	 * used to create configurations to resolve the poms.
	 * @param project the project
	 * @param configurationContainer the configuration container
	 */
	public MavenPomResolver(Project project, DependencyManagementConfigurationContainer configurationContainer) {
		this.configurationContainer = configurationContainer;
		this.effectiveModelBuilder = new EffectiveModelBuilder(project, configurationContainer);
		this.dependencyHandler = project.getDependencies();
	}

	@Override
	public List<Pom> resolvePomsLeniently(List<PomReference> pomReferences) {
		return createPoms(createConfiguration(pomReferences).getResolvedConfiguration().getLenientConfiguration()
				.getArtifacts(Specs.SATISFIES_ALL), pomReferences, new MapPropertySource(Collections.emptyMap()));
	}

	@Override
	public List<Pom> resolvePoms(List<PomReference> pomReferences, PropertySource properties) {
		List<PomReference> deduplicatedPomReferences = deduplicate(pomReferences);
		return createPoms(
				createConfiguration(deduplicatedPomReferences).getResolvedConfiguration().getResolvedArtifacts(),
				deduplicatedPomReferences, properties);
	}

	private List<PomReference> deduplicate(List<PomReference> pomReferences) {
		List<PomReference> deduplicatedReferences = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		for (int i = pomReferences.size() - 1; i >= 0; i--) {
			PomReference pomReference = pomReferences.get(i);
			if (seen.add(createKey(pomReference.getCoordinates().getGroupId(),
					pomReference.getCoordinates().getArtifactId()))) {
				deduplicatedReferences.add(pomReference);
			}
		}
		Collections.reverse(deduplicatedReferences);
		return deduplicatedReferences;
	}

	private Configuration createConfiguration(List<PomReference> pomReferences) {
		Configuration configuration = this.configurationContainer.newConfiguration();
		for (PomReference pomReference : pomReferences) {
			Coordinates coordinates = pomReference.getCoordinates();
			org.gradle.api.artifacts.Dependency dependency = this.dependencyHandler.create(coordinates.getGroupId()
					+ ":" + coordinates.getArtifactId() + ":" + coordinates.getVersion() + "@pom");
			configuration.getDependencies().add(dependency);
		}
		return configuration;
	}

	private List<Pom> createPoms(Set<ResolvedArtifact> resolvedArtifacts, List<PomReference> pomReferences,
			PropertySource properties) {
		Map<String, PomReference> referencesById = new HashMap<>();
		for (PomReference pomReference : pomReferences) {
			referencesById.put(createKey(pomReference.getCoordinates().getGroupId(),
					pomReference.getCoordinates().getArtifactId()), pomReference);
		}
		List<ModelInput> modelInputs = new ArrayList<>();
		for (ResolvedArtifact resolvedArtifact : resolvedArtifacts) {
			ModuleVersionIdentifier id = resolvedArtifact.getModuleVersion().getId();
			PomReference reference = referencesById.get(createKey(id.getGroup(), id.getName()));
			CompositePropertySource allProperties = new CompositePropertySource(reference.getProperties(), properties);
			modelInputs.add(new ModelInput(resolvedArtifact.getFile(), allProperties));
		}
		return createPoms(modelInputs);
	}

	private List<Pom> createPoms(List<ModelInput> inputs) {
		List<Model> effectiveModels = this.effectiveModelBuilder.buildModels(inputs);
		List<Pom> poms = new ArrayList<>(effectiveModels.size());
		for (Model effectiveModel : effectiveModels) {
			Coordinates coordinates = new Coordinates(effectiveModel.getGroupId(), effectiveModel.getArtifactId(),
					effectiveModel.getVersion());
			poms.add(new Pom(coordinates, getManagedDependencies(effectiveModel), getDependencies(effectiveModel),
					asMap(effectiveModel.getProperties())));
		}
		return poms;
	}

	private List<Dependency> getManagedDependencies(Model model) {
		if (model.getDependencyManagement() == null || model.getDependencyManagement().getDependencies() == null) {
			return Collections.emptyList();
		}
		List<Dependency> result = new ArrayList<>();
		for (io.spring.gradle.dependencymanagement.org.apache.maven.model.Dependency dependency : model
				.getDependencyManagement().getDependencies()) {
			result.add(createDependency(dependency));
		}
		return result;
	}

	private Dependency createDependency(
			io.spring.gradle.dependencymanagement.org.apache.maven.model.Dependency dependency) {
		Set<Exclusion> exclusions = new LinkedHashSet<>();
		if (dependency.getExclusions() != null) {
			for (io.spring.gradle.dependencymanagement.org.apache.maven.model.Exclusion exclusion : dependency
					.getExclusions()) {
				exclusions.add(new Exclusion(exclusion.getGroupId(), exclusion.getArtifactId()));
			}
		}
		return new Dependency(
				new Coordinates(dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()),
				dependency.isOptional(), dependency.getType(), dependency.getClassifier(), dependency.getScope(),
				exclusions);
	}

	private List<Dependency> getDependencies(Model model) {
		if (model.getDependencies() == null) {
			return Collections.emptyList();
		}
		List<Dependency> result = new ArrayList<>();
		for (io.spring.gradle.dependencymanagement.org.apache.maven.model.Dependency dependency : model
				.getDependencies()) {
			result.add(createDependency(dependency));
		}
		return result;
	}

	private Map<String, String> asMap(Properties properties) {
		Map<String, String> map = new HashMap<>();
		for (String name : properties.stringPropertyNames()) {
			map.put(name, properties.getProperty(name));
		}
		return map;
	}

	private String createKey(String group, String name) {
		return group + ":" + name;
	}

}
