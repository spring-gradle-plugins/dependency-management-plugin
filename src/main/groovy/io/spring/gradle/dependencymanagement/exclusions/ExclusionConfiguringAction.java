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

package io.spring.gradle.dependencymanagement.exclusions;

import io.spring.gradle.dependencymanagement.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.DependencyManagementContainer;
import io.spring.gradle.dependencymanagement.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.VersionConfiguringAction;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ResolvableDependencies;
import org.gradle.api.artifacts.result.DependencyResult;
import org.gradle.api.artifacts.result.ResolutionResult;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.artifacts.result.ResolvedDependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * An {@link Action} to be applied to {@link ResolvableDependencies} that configures exclusions
 * based on the Maven exclusion metadata gleaned from the dependencies.
 *
 * @author Andy Wilkinson
 */
public class ExclusionConfiguringAction implements Action<ResolvableDependencies> {

    private final Logger log = LoggerFactory.getLogger(ExclusionConfiguringAction.class);

    private final DependencyManagementExtension dependencyManagementExtension;

    private final DependencyManagementContainer dependencyManagementContainer;

    private final DependencyManagementConfigurationContainer configurationContainer;

    private final Configuration configuration;

    private final ExclusionResolver exclusionResolver;

    private final VersionConfiguringAction versionConfiguringAction;

    public ExclusionConfiguringAction(DependencyManagementExtension dependencyManagementExtension,
            DependencyManagementContainer dependencyManagementContainer,
            DependencyManagementConfigurationContainer configurationContainer,
            Configuration configuration, ExclusionResolver exclusionResolver,
            VersionConfiguringAction versionConfiguringAction) {
        this.dependencyManagementExtension = dependencyManagementExtension;
        this.dependencyManagementContainer = dependencyManagementContainer;
        this.configurationContainer = configurationContainer;
        this.configuration = configuration;
        this.exclusionResolver = exclusionResolver;
        this.versionConfiguringAction = versionConfiguringAction;
    }

    @Override
    public void execute(ResolvableDependencies resolvableDependencies) {
        if (this.dependencyManagementExtension.isApplyMavenExclusions()) {
            applyMavenExclusions(resolvableDependencies);
        }

    }

    private void applyMavenExclusions(ResolvableDependencies resolvableDependencies) {
        Set<ResolvedComponentResult> excludedDependencies = findExcludedDependencies();
        if (log.isInfoEnabled()) {
            log.info("Excluding " + String.valueOf(excludedDependencies));
        }

        List<Map<String, String>> exclusions = new ArrayList<Map<String, String>>();
        for (ResolvedComponentResult excludedDependency : excludedDependencies) {
            Map<String, String> exclusion = new HashMap<String, String>();
            exclusion.put("group", excludedDependency.getModuleVersion().getGroup());
            exclusion.put("module", excludedDependency.getModuleVersion().getName());
            exclusions.add(exclusion);
        }
        for (Dependency dependency : resolvableDependencies.getDependencies()) {
            if (dependency instanceof ModuleDependency) {
                for (Map<String, String> exclusion : exclusions) {
                    ((ModuleDependency) dependency).exclude(exclusion);
                }

            }

        }
    }

    private Set<ResolvedComponentResult> findExcludedDependencies() {
        Configuration configurationCopy = this.configurationContainer.newConfiguration(
                configuration.getAllDependencies()
                        .toArray(new Dependency[configuration.getAllDependencies().size()]));
        configurationCopy.getResolutionStrategy().eachDependency(this.versionConfiguringAction);
        ResolutionResult resolutionResult = configurationCopy.getIncoming().getResolutionResult();
        ResolvedComponentResult root = resolutionResult.getRoot();
        Set<ResolvedComponentResult> excludedDependencies = resolutionResult.getAllComponents();
        Set<ResolvedComponentResult> includedDependencies = determineIncludedComponents(root,
                this.exclusionResolver.resolveExclusions(excludedDependencies));
        excludedDependencies.removeAll(includedDependencies);
        return excludedDependencies;
    }

    private Set<ResolvedComponentResult> determineIncludedComponents(ResolvedComponentResult root,
            Map<String, Exclusions> pomExclusionsById) {
        LinkedList<Node> queue = new LinkedList<Node>();
        queue.add(new Node(root, getId(root), new HashSet<String>()));
        Set<ResolvedComponentResult> seen = new HashSet<ResolvedComponentResult>();
        Set<ResolvedComponentResult> includedComponents = new HashSet<ResolvedComponentResult>();
        while (!queue.isEmpty()) {
            Node node = queue.remove();
            includedComponents.add(node.component);
            for (DependencyResult dependency : node.component.getDependencies()) {
                if (dependency instanceof ResolvedDependencyResult) {
                    ResolvedComponentResult child = ((ResolvedDependencyResult) dependency)
                            .getSelected();
                    String childId = getId(child);
                    if (!node.excluded(childId) && seen
                            .add(child)) {
                        queue.add(new Node(child, childId,
                                getChildExclusions(node, childId, pomExclusionsById)));
                    }

                }

            }

        }

        return includedComponents;
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

    private static class Node {

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
}
