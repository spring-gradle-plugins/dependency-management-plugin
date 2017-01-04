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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.component.ComponentSelector;
import org.gradle.api.artifacts.component.ModuleComponentSelector;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.gradle.api.artifacts.result.UnresolvedDependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer.ConfigurationConfigurer;

/**
 * An {@link Action} to be applied to {@link ResolvableDependencies} that configures exclusions based on the Maven
 * exclusion metadata gleaned from the dependencies.
 *
 * @author Andy Wilkinson
 */
class ExclusionConfiguringAction implements Action<ResolvableDependencies> {

    private static final Logger logger = LoggerFactory.getLogger(ExclusionConfiguringAction.class);

    private final DependencyManagementSettings dependencyManagementSettings;

    private final DependencyManagementContainer dependencyManagementContainer;

    private final DependencyManagementConfigurationContainer configurationContainer;

    private final Configuration configuration;

    private final ExclusionResolver exclusionResolver;

    private final ConfigurationConfigurer configurationConfigurer;

    ExclusionConfiguringAction(DependencyManagementSettings dependencyManagementSettings,
            DependencyManagementContainer dependencyManagementContainer,
            DependencyManagementConfigurationContainer configurationContainer,
            Configuration configuration, ExclusionResolver exclusionResolver, ConfigurationConfigurer configurationConfigurer) {
        this.dependencyManagementSettings = dependencyManagementSettings;
        this.dependencyManagementContainer = dependencyManagementContainer;
        this.configurationContainer = configurationContainer;
        this.configuration = configuration;
        this.exclusionResolver = exclusionResolver;
        this.configurationConfigurer = configurationConfigurer;
    }

    @Override
    public void execute(ResolvableDependencies resolvableDependencies) {
        if (this.dependencyManagementSettings.isApplyMavenExclusions()) {
            applyMavenExclusions(resolvableDependencies);
        }
    }

    private void applyMavenExclusions(ResolvableDependencies resolvableDependencies) {
        Set<DependencyCandidate> excludedDependencies = findExcludedDependencies();
        if (logger.isInfoEnabled()) {
            logger.info("Excluding " + excludedDependencies);
        }

        List<Map<String, String>> exclusions = new ArrayList<Map<String, String>>();
        for (DependencyCandidate excludedDependency : excludedDependencies) {
            Map<String, String> exclusion = new HashMap<String, String>();
            exclusion.put("group", excludedDependency.groupId);
            exclusion.put("module", excludedDependency.artifactId);
            exclusions.add(exclusion);
        }
        for (org.gradle.api.artifacts.Dependency dependency : resolvableDependencies.getDependencies()) {
            if (dependency instanceof ModuleDependency) {
                for (Map<String, String> exclusion : exclusions) {
                    ((ModuleDependency) dependency).exclude(exclusion);
                }


            }

        }
    }

    private Set<DependencyCandidate> findExcludedDependencies() {
        DependencySet allDependencies = this.configuration.getAllDependencies();
        Configuration configurationCopy = this.configurationContainer.newConfiguration(
                this.configurationConfigurer,
                allDependencies.toArray(new org.gradle.api.artifacts.Dependency[allDependencies.size()]));
        ResolutionResult resolutionResult = configurationCopy.getIncoming().getResolutionResult();
        ResolvedComponentResult root = resolutionResult.getRoot();
        final Set<DependencyCandidate> excludedDependencies = new HashSet<DependencyCandidate>();
        resolutionResult.allDependencies(new Action<DependencyResult>() {
            @Override
            public void execute(DependencyResult dependencyResult) {
                if (dependencyResult instanceof ResolvedDependencyResult) {
                    ResolvedDependencyResult resolved = (ResolvedDependencyResult) dependencyResult;
                    excludedDependencies.add(new DependencyCandidate(resolved.getSelected()
                            .getModuleVersion().getGroup(), resolved.getSelected()
                            .getModuleVersion().getName()));
                }
                else if (dependencyResult instanceof UnresolvedDependencyResult) {
                    DependencyCandidate dependencyCandidate =
                            toDependencyCandidate((UnresolvedDependencyResult) dependencyResult);
                    if (dependencyCandidate != null) {
                        excludedDependencies.add(dependencyCandidate);
                    }
                }
            }
        });
        Set<DependencyCandidate> includedDependencies = determineIncludedComponents(root,
                this.exclusionResolver.resolveExclusions(resolutionResult.getAllComponents()));
        excludedDependencies.removeAll(includedDependencies);
        return excludedDependencies;
    }

    private Set<DependencyCandidate> determineIncludedComponents(ResolvedComponentResult root,
            Map<String, Exclusions> pomExclusionsById) {
        LinkedList<Node> queue = new LinkedList<Node>();
        queue.add(new Node(root, getId(root), new HashSet<String>()));
        Set<ResolvedComponentResult> seen = new HashSet<ResolvedComponentResult>();
        Set<DependencyCandidate> includedComponents = new HashSet<DependencyCandidate>();
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            includedComponents.add(new DependencyCandidate(node.component.getModuleVersion()
                    .getGroup(), node.component.getModuleVersion().getName()));
            for (DependencyResult dependency : node.component.getDependencies()) {
                if (dependency instanceof ResolvedDependencyResult) {
                    handleResolvedDependency((ResolvedDependencyResult) dependency, node, pomExclusionsById, queue,
                            seen);
                }
                else if (dependency instanceof UnresolvedDependencyResult) {
                    handleUnresolvedDependency((UnresolvedDependencyResult) dependency, node, includedComponents);
                }
            }
        }
        return includedComponents;
    }

    private void handleResolvedDependency(ResolvedDependencyResult dependency, Node node,
            Map<String, Exclusions> pomExclusionsById, LinkedList<Node> queue, Set<ResolvedComponentResult> seen) {
        ResolvedComponentResult child = dependency
                .getSelected();
        String childId = getId(child);
        if (!node.excluded(childId) && seen.add(child)) {
            queue.add(new Node(child, childId, getChildExclusions(node, childId, pomExclusionsById)));
        }
    }

    private void handleUnresolvedDependency(UnresolvedDependencyResult dependency, Node node,
            Set<DependencyCandidate> includedComponents) {
        DependencyCandidate dependencyCandidate = toDependencyCandidate(
                dependency);
        if (dependencyCandidate != null && (!node.excluded(dependencyCandidate.groupId + ":" +
                dependencyCandidate.artifactId))) {
            includedComponents.add(dependencyCandidate);
        }
    }

    private DependencyCandidate toDependencyCandidate(
            UnresolvedDependencyResult unresolved) {
        ComponentSelector attemptedSelector = unresolved.getAttempted();
        if (!(attemptedSelector instanceof ModuleComponentSelector)) {
            return null;
        }
        ModuleComponentSelector attemptedModuleSelector =
                (ModuleComponentSelector) attemptedSelector;
        return new DependencyCandidate(attemptedModuleSelector
                .getGroup(), attemptedModuleSelector.getModule());
    }

    private Set<String> getChildExclusions(Node parent, String childId,
            Map<String, Exclusions> pomExclusionsById) {
        Set<String> childExclusions = new HashSet<String>(parent.exclusions);
        addAllIfPossible(childExclusions,
                this.dependencyManagementContainer.getExclusions(this.configuration)
                        .exclusionsForDependency(childId));
        Exclusions exclusionsInPom = pomExclusionsById.get(parent.id);
        if (exclusionsInPom != null) {
            addAllIfPossible(childExclusions, exclusionsInPom.exclusionsForDependency(childId));
        }
        return childExclusions;
    }

    private void addAllIfPossible(Set<String> current, Set<String> addition) {
        if (addition != null) {
            current.addAll(addition);
        }

    }

    private String getId(ResolvedComponentResult component) {
        return component.getModuleVersion().getGroup() + ":" + component.getModuleVersion()
                .getName();
    }

    private static final class Node {

        private final ResolvedComponentResult component;

        private final String id;

        private final Set<String> exclusions;

        private Node(ResolvedComponentResult component, String id, Set<String> exclusions) {
            this.component = component;
            this.id = id;
            this.exclusions = exclusions;
        }

        private boolean excluded(String id) {
            return this.exclusions != null && this.exclusions.contains(id);
        }

    }

    private static final class DependencyCandidate {

        private final String groupId;

        private final String artifactId;

        private DependencyCandidate(String groupId, String artifactId) {
            this.groupId = groupId;
            this.artifactId = artifactId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DependencyCandidate that = (DependencyCandidate) o;

            if (!this.groupId.equals(that.groupId)) {
                return false;
            }
            return this.artifactId.equals(that.artifactId);

        }

        @Override
        public int hashCode() {
            int result = this.groupId.hashCode();
            result = 31 * result + this.artifactId.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return this.groupId + ":" + this.artifactId;
        }

    }
}
