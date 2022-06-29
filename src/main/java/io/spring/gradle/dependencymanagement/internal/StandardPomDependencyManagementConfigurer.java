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

package io.spring.gradle.dependencymanagement.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.util.Node;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings.PomCustomizationSettings;
import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;
import io.spring.gradle.dependencymanagement.internal.pom.Dependency;
import io.spring.gradle.dependencymanagement.internal.pom.Pom;
import io.spring.gradle.dependencymanagement.internal.pom.PomReference;
import io.spring.gradle.dependencymanagement.internal.pom.PomResolver;
import io.spring.gradle.dependencymanagement.internal.properties.ProjectPropertySource;
import io.spring.gradle.dependencymanagement.internal.properties.PropertySource;
import io.spring.gradle.dependencymanagement.maven.PomDependencyManagementConfigurer;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;

/**
 * Standard implementation of {@link PomDependencyManagementConfigurer}.
 *
 * @author Andy Wilkinson
 */
public class StandardPomDependencyManagementConfigurer implements PomDependencyManagementConfigurer {

	private static final String NODE_NAME_DEPENDENCY_MANAGEMENT = "dependencyManagement";

	private static final String NODE_NAME_DEPENDENCIES = "dependencies";

	private static final String NODE_NAME_DEPENDENCY = "dependency";

	private static final String NODE_NAME_EXCLUSION = "exclusion";

	private static final String NODE_NAME_GROUP_ID = "groupId";

	private static final String NODE_NAME_ARTIFACT_ID = "artifactId";

	private static final String NODE_NAME_EXCLUSIONS = "exclusions";

	private static final String NODE_NAME_VERSION = "version";

	private static final String NODE_NAME_SCOPE = "scope";

	private static final String NODE_NAME_TYPE = "type";

	private static final String NODE_NAME_CLASSIFIER = "classifier";

	private final DependencyManagement dependencyManagement;

	private final PomCustomizationSettings settings;

	private final PomResolver pomResolver;

	private final Project project;

	/**
	 * Creates a new {@code StandardPomDependencyManagementConfigurer} that will configure
	 * the pom's dependency management to reflect the given {@code dependencyManagement}.
	 * The given {@code settings} will control how the dependency management is applied to
	 * the pom.
	 * @param dependencyManagement the dependency management
	 * @param settings the customization settings
	 * @param pomResolver resolves imported boms during dependency management
	 * configuration
	 * @param project owner of the pom that is being configured
	 */
	public StandardPomDependencyManagementConfigurer(DependencyManagement dependencyManagement,
			PomCustomizationSettings settings, PomResolver pomResolver, Project project) {
		this.dependencyManagement = dependencyManagement;
		this.settings = settings;
		this.pomResolver = pomResolver;
		this.project = project;
	}

	@Override
	public void execute(XmlProvider xmlProvider) {
		configurePom(xmlProvider.asNode());
	}

	@Override
	public void configurePom(Node pom) {
		if (this.settings.isEnabled()) {
			doConfigurePom(pom);
		}
	}

	private void doConfigurePom(Node pom) {
		Node dependencyManagementNode = findChild(pom, NODE_NAME_DEPENDENCY_MANAGEMENT);
		if (dependencyManagementNode == null) {
			dependencyManagementNode = pom.appendNode(NODE_NAME_DEPENDENCY_MANAGEMENT);
		}
		Node managedDependenciesNode = findChild(dependencyManagementNode, NODE_NAME_DEPENDENCIES);
		if (managedDependenciesNode == null) {
			managedDependenciesNode = dependencyManagementNode.appendNode(NODE_NAME_DEPENDENCIES);
		}
		configureBomImports(managedDependenciesNode);
		configureManagedDependencies(managedDependenciesNode, findChild(pom, NODE_NAME_DEPENDENCIES));
	}

	private Node findChild(Node node, String name) {
		for (Object childObject : node.children()) {
			if ((childObject instanceof Node) && ((Node) childObject).name().equals(name)) {
				return (Node) childObject;
			}

		}
		return null;
	}

	private void configureBomImports(Node dependencies) {
		List<PomReference> bomReferences = this.dependencyManagement.getImportedBomReferences();
		Map<String, Dependency> withoutPropertiesManagedDependencies = getManagedDependenciesById(bomReferences,
				new EmptyPropertySource());
		Map<String, Dependency> withPropertiesManagedDependencies = getManagedDependenciesById(bomReferences,
				new ProjectPropertySource(this.project));
		List<Dependency> overrides = new ArrayList<Dependency>();
		for (Map.Entry<String, Dependency> withPropertyEntry : withPropertiesManagedDependencies.entrySet()) {
			Dependency withoutPropertyDependency = withoutPropertiesManagedDependencies.get(withPropertyEntry.getKey());
			if (differentVersions(withoutPropertyDependency, withPropertyEntry.getValue())) {
				overrides.add(withPropertyEntry.getValue());
			}
		}
		for (Dependency override : overrides) {
			appendDependencyNode(dependencies, override.getCoordinates(), override.getScope(), override.getType());
		}
		List<PomReference> importedBoms = this.dependencyManagement.getImportedBomReferences();
		Collections.reverse(importedBoms);
		for (PomReference resolvedBom : importedBoms) {
			addImport(dependencies, resolvedBom);
		}
	}

	private Map<String, Dependency> getManagedDependenciesById(List<PomReference> bomReferences,
			PropertySource propertySource) {
		Map<String, Dependency> managedDependencies = new HashMap<String, Dependency>();
		for (Pom pom : this.pomResolver.resolvePoms(bomReferences, propertySource)) {
			for (Dependency dependency : pom.getManagedDependencies()) {
				managedDependencies.put(createId(dependency), dependency);
			}
		}
		return managedDependencies;
	}

	private String createId(Dependency dependency) {
		return String.format("%s:%s:%s:%s:%s", dependency.getCoordinates().getGroupId(),
				dependency.getCoordinates().getArtifactId(), dependency.getScope(), dependency.getType(),
				dependency.getClassifier());
	}

	private boolean differentVersions(Dependency dependency1, Dependency dependency2) {
		if (dependency1 == null) {
			return true;
		}
		String version1 = dependency1.getCoordinates().getVersion();
		String version2 = dependency2.getCoordinates().getVersion();
		return !version1.equals(version2);
	}

	private void addImport(Node dependencies, PomReference importedBom) {
		appendDependencyNode(dependencies, importedBom.getCoordinates(), "import", "pom");
	}

	private Node appendDependencyNode(Node parent, Coordinates coordinates, String scope, String type) {
		Node dependencyNode = parent.appendNode(NODE_NAME_DEPENDENCY);
		dependencyNode.appendNode(NODE_NAME_GROUP_ID, coordinates.getGroupId());
		dependencyNode.appendNode(NODE_NAME_ARTIFACT_ID, coordinates.getArtifactId());
		dependencyNode.appendNode(NODE_NAME_VERSION, coordinates.getVersion());
		if (scope != null) {
			dependencyNode.appendNode(NODE_NAME_SCOPE, scope);
		}
		if (!"jar".equals(type)) {
			dependencyNode.appendNode(NODE_NAME_TYPE, type);
		}
		return dependencyNode;
	}

	private void configureManagedDependencies(Node managedDependencies, Node dependencies) {
		for (Dependency managedDependency : this.dependencyManagement.getManagedDependencies()) {
			addManagedDependency(managedDependencies, managedDependency, null);
			if (dependencies != null) {
				for (String classifier : findClassifiers(dependencies, managedDependency)) {
					addManagedDependency(managedDependencies, managedDependency, classifier);
				}
			}
		}
	}

	private void addManagedDependency(Node managedDependencies, Dependency managedDependency, String classifier) {
		Node dependencyNode = appendDependencyNode(managedDependencies, managedDependency.getCoordinates(),
				managedDependency.getScope(), managedDependency.getType());
		if (!managedDependency.getExclusions().isEmpty()) {
			Node exclusionsNode = dependencyNode.appendNode(NODE_NAME_EXCLUSIONS);
			for (Exclusion exclusion : managedDependency.getExclusions()) {
				Node exclusionNode = exclusionsNode.appendNode(NODE_NAME_EXCLUSION);
				exclusionNode.appendNode(NODE_NAME_GROUP_ID, exclusion.getGroupId());
				exclusionNode.appendNode(NODE_NAME_ARTIFACT_ID, exclusion.getArtifactId());
			}
		}
		if (classifier != null) {
			Node classifierNode = dependencyNode.appendNode(NODE_NAME_CLASSIFIER);
			classifierNode.setValue(classifier);
		}
	}

	private List<String> findClassifiers(Node dependencies, Dependency managedDependency) {
		List<String> classifiers = new ArrayList<String>();
		for (Object child : dependencies.children()) {
			if (child instanceof Node && ((Node) child).name().equals(NODE_NAME_DEPENDENCY)) {
				Node dependency = (Node) child;
				String groupId = findTextOfChild(dependency, NODE_NAME_GROUP_ID);
				String artifactId = findTextOfChild(dependency, NODE_NAME_ARTIFACT_ID);
				if (managedDependency.getCoordinates().getGroupId().equals(groupId)
						&& managedDependency.getCoordinates().getArtifactId().equals(artifactId)) {
					String classifier = findTextOfChild(dependency, NODE_NAME_CLASSIFIER);
					if (classifier != null && classifier.length() > 0) {
						classifiers.add(classifier);
					}
				}
			}
		}
		return classifiers;
	}

	private String findTextOfChild(Node node, String name) {
		Node child = findChild(node, name);
		return (child != null) ? child.text() : null;
	}

	private static final class EmptyPropertySource implements PropertySource {

		@Override
		public Object getProperty(String name) {
			return null;
		}

	}

}
