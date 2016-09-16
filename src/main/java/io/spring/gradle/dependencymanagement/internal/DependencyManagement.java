/*
 * Copyright 2014-2016 the original author or authors.
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

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;
import io.spring.gradle.dependencymanagement.internal.pom.Dependency;
import io.spring.gradle.dependencymanagement.internal.pom.Pom;
import io.spring.gradle.dependencymanagement.internal.pom.PomReference;
import io.spring.gradle.dependencymanagement.internal.pom.PomResolver;

/**
 * Encapsulates dependency management information for a particular configuration in a Gradle project.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagement {

    private static final Logger logger = LoggerFactory.getLogger(DependencyManagement.class);

    private final Project project;

    private final Configuration targetConfiguration;

    private final PomResolver pomResolver;

    private boolean resolved;

    private final Map<String, String> versions = new HashMap<String, String>();

    private final Map<String, String> explicitVersions = new HashMap<String, String>();

    private final Exclusions explicitExclusions = new Exclusions();

    private final Exclusions allExclusions = new Exclusions();

    private final Map<String, String> bomProperties = new HashMap<String, String>();

    private final List<PomReference> importedBoms = new ArrayList<PomReference>();

    private List<Pom> resolvedBoms = new ArrayList<Pom>();

    DependencyManagement(Project project, PomResolver pomResolver) {
        this(project, null, pomResolver);
    }

    DependencyManagement(Project project, Configuration targetConfiguration, PomResolver pomResolver) {
        this.project = project;
        this.pomResolver = pomResolver;
        this.targetConfiguration = targetConfiguration;
    }

    void importBom(Coordinates coordinates, Map<String, String> properties) {
        this.importedBoms.add(new PomReference(coordinates, properties));
    }

    /**
     * Returns the {@link Pom Poms} that have been imported.
     *
     * @return the imported poms
     */
    public List<Pom> getImportedBoms() {
        resolveIfNecessary();
        return this.resolvedBoms;
    }

    Map<String, String> getImportedProperties() {
        resolveIfNecessary();
        return this.bomProperties;
    }

    void addImplicitManagedVersion(String group, String name, String version) {
        this.versions.put(createKey(group, name), version);
    }

    void addExplicitManagedVersion(String group, String name, String version, List<String>
            exclusions) {
        String key = createKey(group, name);
        this.explicitVersions.put(key, version);
        this.explicitExclusions.add(key, exclusions);
        this.allExclusions.add(key, exclusions);
        addImplicitManagedVersion(group, name, version);
    }

    String getManagedVersion(String group, String name) {
        resolveIfNecessary();
        return this.versions.get(createKey(group, name));
    }

    Map<String, String> getManagedVersions() {
        resolveIfNecessary();
        return new HashMap<String, String>(this.versions);
    }

    /**
     * Returns the managed dependencies.
     *
     * @return the managed dependencies
     */
    public List<Dependency> getManagedDependencies() {
        List<Dependency> managedDependencies = new ArrayList<Dependency>();
        for (Map.Entry<String, String> entry: this.explicitVersions.entrySet()) {
            String[] components = entry.getKey().split(":");
            managedDependencies.add(new Dependency(new Coordinates(components[0], components[1],
                    entry.getValue()), this.explicitExclusions.exclusionsForDependency(entry.getKey())));
        }
        return managedDependencies;
    }

    private String createKey(String group, String name) {
        return group + ":" + name;
    }

    Exclusions getExclusions() {
        resolveIfNecessary();
        return this.allExclusions;
    }

    private void resolveIfNecessary() {
        if (!this.resolved) {
            try {
                this.resolved = true;
                resolve();
            }
            catch (Exception ex) {
                throw new GradleException("Failed to resolve imported Maven boms: " +
                        getRootCause(ex).getMessage(), ex);
            }
        }
    }

    private Throwable getRootCause(Exception ex) {
        Throwable candidate = ex;
        while (candidate.getCause() != null) {
            candidate = candidate.getCause();
        }
        return candidate;
    }

    private void resolve() {
        if (this.targetConfiguration != null) {
            logger.info("Resolving dependency management for configuration '{}' of project '{}'",
                    this.targetConfiguration.getName(), this.project.getName());
        }
        else {
            logger.info("Resolving global dependency management for project '{}'", this.project.getName());
        }
        Map<String, String> existingVersions = new HashMap<String, String>();
        existingVersions.putAll(this.versions);

        logger.debug("Preserving existing versions: {}", existingVersions);

        this.resolvedBoms = this.pomResolver.resolvePoms(this.importedBoms);

        for (Pom resolvedBom: this.resolvedBoms) {
            for (Dependency dependency : resolvedBom.getManagedDependencies()) {
                Coordinates coordinates = dependency.getCoordinates();
                this.versions.put(coordinates.getGroupId() + ":" + coordinates.getArtifactId(),
                        coordinates.getVersion());
                this.allExclusions.add(coordinates.getGroupId() + ":" + coordinates.getArtifactId(),
                        dependency.getExclusions());
            }
            this.bomProperties.putAll(resolvedBom.getProperties());
        }

        this.versions.putAll(existingVersions);
    }

}
