/*
 * Copyright 2014-2023 the original author or authors.
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

import groovy.namespace.QName;
import groovy.util.Node;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings.PomCustomizationSettings;
import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;
import io.spring.gradle.dependencymanagement.internal.pom.Dependency;
import io.spring.gradle.dependencymanagement.internal.pom.Pom;
import io.spring.gradle.dependencymanagement.internal.pom.PomReference;
import io.spring.gradle.dependencymanagement.maven.PomDependencyManagementConfigurer;
import org.gradle.api.XmlProvider;

/**
 * Standard implementation of {@link PomDependencyManagementConfigurer}.
 *
 * @author Andy Wilkinson
 * @author Rupert Waldron
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

	private final List<Dependency> managedDependencies;

	private final List<PomReference> importedBomReferences;

	private final List<Pom> resolvedBomReferencesWithoutProperties;

	private final List<Pom> resolvedBomReferencesWithProperties;

	private final PomCustomizationSettings settings;

	/**
	 * Creates a new {@code StandardPomDependencyManagementConfigurer} that will configure
	 * the pom's dependency management to reflect the given {@code dependencyManagement}.
	 * The given {@code settings} will control how the dependency management is applied to
	 * the pom.
	 * @param managedDependencies the managed dependencies
	 * @param importedBomReferences the bom references
	 * @param resolvedBomReferencesWithoutProperties the resolved bom references without
	 * properties
	 * @param resolvedBomReferencesWithProperties the resolved bom references with
	 * properties
	 * @param settings the customization settings
	 */
	public StandardPomDependencyManagementConfigurer(List<Dependency> managedDependencies,
			List<PomReference> importedBomReferences, List<Pom> resolvedBomReferencesWithoutProperties,
			List<Pom> resolvedBomReferencesWithProperties, PomCustomizationSettings settings) {
		this.managedDependencies = managedDependencies;
		this.importedBomReferences = importedBomReferences;
		this.resolvedBomReferencesWithoutProperties = resolvedBomReferencesWithoutProperties;
		this.resolvedBomReferencesWithProperties = resolvedBomReferencesWithProperties;
		this.settings = settings;
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
			if (childObject instanceof Node) {
				Node childNode = (Node) childObject;
				if (hasName(childNode, name)) {
					return childNode;
				}
			}
		}
		return null;
	}

	private boolean hasName(Node node, String wanted) {
		Object actual = node.name();
		return (actual instanceof QName && ((QName) actual).getLocalPart().equals(wanted)) || actual.equals(wanted);
	}

	private void configureBomImports(Node dependencies) {
		Map<String, Dependency> withoutPropertiesManagedDependencies = getManagedDependenciesById(
				this.resolvedBomReferencesWithoutProperties);
		Map<String, Dependency> withPropertiesManagedDependencies = getManagedDependenciesById(
				this.resolvedBomReferencesWithProperties);
		List<Dependency> overrides = new ArrayList<>();
		for (Map.Entry<String, Dependency> withPropertyEntry : withPropertiesManagedDependencies.entrySet()) {
			Dependency withoutPropertyDependency = withoutPropertiesManagedDependencies.get(withPropertyEntry.getKey());
			if (differentVersions(withoutPropertyDependency, withPropertyEntry.getValue())) {
				overrides.add(withPropertyEntry.getValue());
			}
		}
		for (Dependency override : overrides) {
			appendDependencyNode(dependencies, override.getCoordinates(), override.getScope(), override.getType());
		}
		List<PomReference> importOrderBomReferences = new ArrayList<>(this.importedBomReferences);
		Collections.reverse(importOrderBomReferences);
		for (PomReference bomReference : importOrderBomReferences) {
			addImport(dependencies, bomReference);
		}
	}

	private Map<String, Dependency> getManagedDependenciesById(List<Pom> resolvedReferences) {
		Map<String, Dependency> managedDependencies = new HashMap<>();
		for (Pom pom : resolvedReferences) {
			for (Dependency dependency : pom.getManagedDependencies()) {
				managedDependencies.put(createId(dependency), dependency);
			}
		}
		return managedDependencies;
	}

	private String createId(Dependency dependency) {
		Coordinates coordinates = dependency.getCoordinates();
		return String.format("%s:%s:%s:%s", coordinates.getGroupAndArtifactId(), dependency.getScope(),
				dependency.getType(), dependency.getClassifier());
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
		for (Dependency managedDependency : this.managedDependencies) {
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
		List<String> classifiers = new ArrayList<>();
		for (Object child : dependencies.children()) {
			if (child instanceof Node && hasName((Node) child, NODE_NAME_DEPENDENCY)) {
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

}
