/*
 * Copyright 2014-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import groovy.util.Node;
import org.gradle.api.Project;
import org.gradle.api.XmlProvider;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings.PomCustomizationSettings;
import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;
import io.spring.gradle.dependencymanagement.internal.pom.Dependency;
import io.spring.gradle.dependencymanagement.internal.pom.Pom;
import io.spring.gradle.dependencymanagement.internal.pom.PomReference;
import io.spring.gradle.dependencymanagement.internal.pom.PomResolver;
import io.spring.gradle.dependencymanagement.internal.properties.ProjectPropertySource;
import io.spring.gradle.dependencymanagement.internal.properties.PropertySource;
import io.spring.gradle.dependencymanagement.maven.PomDependencyManagementConfigurer;

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

    private final DependencyManagement dependencyManagement;

    private final PomCustomizationSettings settings;

    private final PomResolver pomResolver;

    private final Project project;

    /**
     * Creates a new {@code StandardPomDependencyManagementConfigurer} that will configure the pom's dependency management
     * to reflect the given {@code dependencyManagement}. The given {@code settings} will control how the
     * dependency management is applied to the pom.
     *
     * @param dependencyManagement the dependency management
     * @param settings the customization settings
     * @param pomResolver resolves imported boms during dependency management configuration
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
        Node dependenciesNode = findChild(dependencyManagementNode, NODE_NAME_DEPENDENCIES);
        if (dependenciesNode == null) {
            dependenciesNode = dependencyManagementNode.appendNode(NODE_NAME_DEPENDENCIES);
        }
        configureBomImports(dependenciesNode);
        configureDependencies(dependenciesNode);
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
        List<Pom> resolvedWithoutPropertiesBoms = this.pomResolver.resolvePoms(bomReferences, new PropertySource() {

            @Override
            public Object getProperty(String name) {
                return null;
            }

        });
        Map<String, Dependency> managedDependencies = new HashMap<String, Dependency>();
        for (Pom pomWithoutProperty: resolvedWithoutPropertiesBoms) {
            for (Dependency dependency: pomWithoutProperty.getManagedDependencies()) {
                managedDependencies.put(createId(dependency), dependency);
            }
        }
        List<Dependency> overrides = new ArrayList<Dependency>();
        for (Pom pom: this.pomResolver.resolvePoms(bomReferences, new ProjectPropertySource(this.project))) {
            for (Dependency dependency: pom.getManagedDependencies()) {
                Dependency other = managedDependencies.get(createId(dependency));
                if (other ==  null || !dependency.getCoordinates().getVersion().equals(other.getCoordinates().getVersion())) {
                    overrides.add(dependency);
                }
            }
        }
        for (Dependency override: overrides) {
            appendDependencyNode(dependencies, override.getCoordinates(), override.getScope(), override.getType());
        }
        for (Pom resolvedBom: this.dependencyManagement.getImportedBoms()) {
            addImport(dependencies, resolvedBom);
        }
    }

    private String createId(Dependency dependency) {
        return String.format("%s:%s:%s:%s:%s",
                dependency.getCoordinates().getGroupId(),
                dependency.getCoordinates().getArtifactId(),
                dependency.getScope(),
                dependency.getType(),
                dependency.getClassifier());
    }

    private void addImport(Node dependencies, Pom importedBom) {
        appendDependencyNode(dependencies, importedBom.getCoordinates(), "import", "pom");
    }

    private Node appendDependencyNode(Node parent, Coordinates coordinates, String scope,
            String type) {
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

    private void configureDependencies(Node dependencies) {
        for (Dependency dependency : this.dependencyManagement
                .getManagedDependencies()) {
            Node dependencyNode = appendDependencyNode(dependencies, dependency.getCoordinates(), dependency.getScope(),
                    dependency.getType());
            if (!dependency.getExclusions().isEmpty()) {
                Node exclusionsNode = dependencyNode.appendNode(NODE_NAME_EXCLUSIONS);
                for (String exclusion : dependency.getExclusions()) {
                    String[] exclusionComponents = exclusion.split(":");
                    Node exclusionNode = exclusionsNode.appendNode(NODE_NAME_EXCLUSION);
                    exclusionNode.appendNode(NODE_NAME_GROUP_ID, exclusionComponents[0]);
                    exclusionNode.appendNode(NODE_NAME_ARTIFACT_ID, exclusionComponents[1]);
                }

            }
        }
    }

}
