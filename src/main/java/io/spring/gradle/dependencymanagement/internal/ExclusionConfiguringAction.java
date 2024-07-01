/*
 * Copyright 2014-2024 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer.ConfigurationConfigurer;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyConstraintSet;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
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

/**
 * An {@link Action} to be applied to {@link ResolvableDependencies} that configures
 * exclusions based on the Maven exclusion metadata gleaned from the dependencies.
 *
 * @author Andy Wilkinson
 */
class ExclusionConfiguringAction implements Action<DependencySet> {

	private static final Logger logger = LoggerFactory.getLogger(ExclusionConfiguringAction.class);

	private final DependencyManagementSettings dependencyManagementSettings;

	private final DependencyManagementContainer dependencyManagementContainer;

	private final DependencyManagementConfigurationContainer configurationContainer;

	private final Configuration configuration;

	private final ExclusionResolver exclusionResolver;

	private final ConfigurationConfigurer configurationConfigurer;

	ExclusionConfiguringAction(DependencyManagementSettings dependencyManagementSettings,
			DependencyManagementContainer dependencyManagementContainer,
			DependencyManagementConfigurationContainer configurationContainer, Configuration configuration,
			ExclusionResolver exclusionResolver, ConfigurationConfigurer configurationConfigurer) {
		this.dependencyManagementSettings = dependencyManagementSettings;
		this.dependencyManagementContainer = dependencyManagementContainer;
		this.configurationContainer = configurationContainer;
		this.configuration = configuration;
		this.exclusionResolver = exclusionResolver;
		this.configurationConfigurer = configurationConfigurer;
	}

	@Override
	public void execute(DependencySet dependencySet) {
		if (this.configuration.isCanBeResolved() && this.configuration.isTransitive()
				&& this.dependencyManagementSettings.isApplyMavenExclusions()) {
			applyMavenExclusions(dependencySet);
		}
	}

	private void applyMavenExclusions(DependencySet dependencySet) {
		Set<DependencyCandidate> excludedDependencies = findExcludedDependencies();
		logger.info("Excluding {}", excludedDependencies);
		for (DependencyCandidate excludedDependency : excludedDependencies) {
			this.configuration.exclude(excludedDependency.asMap());
		}
	}

	private Set<DependencyCandidate> findExcludedDependencies() {
		ResolutionResult resolutionResult = copyConfiguration().getIncoming().getResolutionResult();
		ResolvedComponentResult root = resolutionResult.getRoot();
		Set<DependencyCandidate> excludedDependencies = new HashSet<>();
		resolutionResult.allDependencies((dependencyResult) -> {
			if (dependencyResult instanceof ResolvedDependencyResult) {
				ResolvedDependencyResult resolved = (ResolvedDependencyResult) dependencyResult;
				if (!resolved.isConstraint()) {
					excludedDependencies.add(new DependencyCandidate(resolved.getSelected().getModuleVersion()));
				}
			}
			else if (dependencyResult instanceof UnresolvedDependencyResult) {
				DependencyCandidate dependencyCandidate = toDependencyCandidate(
						(UnresolvedDependencyResult) dependencyResult);
				if (dependencyCandidate != null) {
					excludedDependencies.add(dependencyCandidate);
				}
			}
		});
		Set<DependencyCandidate> includedDependencies = determineIncludedComponents(root,
				this.exclusionResolver.resolveExclusions(resolutionResult.getAllComponents()));
		excludedDependencies.removeAll(includedDependencies);
		return excludedDependencies;
	}

	private Configuration copyConfiguration() {
		DependencySet allDependencies = this.configuration.getAllDependencies();
		Configuration configurationCopy = this.configurationContainer.newConfiguration(this.configurationConfigurer,
				allDependencies.toArray(new Dependency[allDependencies.size()]));
		DependencyConstraintSet constraints = this.configuration.getAllDependencyConstraints();
		configurationCopy.getDependencyConstraints().addAll(constraints);
		return configurationCopy;
	}

	private Set<DependencyCandidate> determineIncludedComponents(ResolvedComponentResult root,
			Map<String, Exclusions> pomExclusionsById) {
		LinkedList<Node> queue = new LinkedList<>();
		queue.add(new Node(root, getId(root), new HashSet<>()));
		Set<ResolvedComponentResult> seen = new HashSet<>();
		Set<DependencyCandidate> includedComponents = new HashSet<>();
		while (!queue.isEmpty()) {
			Node node = queue.remove();
			includedComponents.add(new DependencyCandidate(node.component.getModuleVersion()));
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
		ResolvedComponentResult child = dependency.getSelected();
		String childId = getId(child);
		if (!node.excluded(childId) && !dependency.isConstraint() && seen.add(child)) {
			queue.add(new Node(child, childId, getChildExclusions(node, childId, pomExclusionsById)));
		}
	}

	private void handleUnresolvedDependency(UnresolvedDependencyResult dependency, Node node,
			Set<DependencyCandidate> includedComponents) {
		DependencyCandidate dependencyCandidate = toDependencyCandidate(dependency);
		if (dependencyCandidate != null && (!node.excluded(dependencyCandidate.getGroupAndArtifactId()))) {
			includedComponents.add(dependencyCandidate);
		}
	}

	private DependencyCandidate toDependencyCandidate(UnresolvedDependencyResult unresolved) {
		ComponentSelector attemptedSelector = unresolved.getAttempted();
		if (!(attemptedSelector instanceof ModuleComponentSelector)) {
			return null;
		}
		ModuleComponentSelector attemptedModuleSelector = (ModuleComponentSelector) attemptedSelector;
		return new DependencyCandidate(attemptedModuleSelector.getGroup(), attemptedModuleSelector.getModule());
	}

	private Set<Exclusion> getChildExclusions(Node parent, String childId, Map<String, Exclusions> pomExclusionsById) {
		Set<Exclusion> childExclusions = new HashSet<>(parent.exclusions);
		addAllIfPossible(childExclusions,
				this.dependencyManagementContainer.getExclusions(this.configuration).exclusionsForDependency(childId));
		Exclusions exclusionsInPom = pomExclusionsById.get(parent.id);
		if (exclusionsInPom != null) {
			addAllIfPossible(childExclusions, exclusionsInPom.exclusionsForDependency(childId));
		}
		return childExclusions;
	}

	private void addAllIfPossible(Set<Exclusion> current, Set<Exclusion> addition) {
		if (addition != null) {
			current.addAll(addition);
		}
	}

	private String getId(ResolvedComponentResult component) {
		return component.getModuleVersion().getGroup() + ":" + component.getModuleVersion().getName();
	}

	private static final class Node {

		private final ResolvedComponentResult component;

		private final String id;

		private final Set<Exclusion> exclusions;

		private Node(ResolvedComponentResult component, String id, Set<Exclusion> exclusions) {
			this.component = component;
			this.id = id;
			this.exclusions = exclusions;
		}

		private boolean excluded(String id) {
			if (this.exclusions == null || this.exclusions.isEmpty()) {
				return false;
			}
			String[] components = id.split(":");
			for (Exclusion exclusion : this.exclusions) {
				if (matches(components[0], exclusion.getGroupId())
						&& matches(components[1], exclusion.getArtifactId())) {
					return true;
				}
			}
			return false;
		}

		private boolean matches(String candidate, String exclusion) {
			return exclusion.equals("*") || exclusion.equals(candidate);
		}

	}

	private static final class DependencyCandidate {

		private final String groupId;

		private final String artifactId;

		private DependencyCandidate(ModuleVersionIdentifier identifier) {
			this(identifier.getGroup(), identifier.getName());
		}

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
			DependencyCandidate other = (DependencyCandidate) o;
			boolean result = this.groupId.equals(other.groupId);
			result = result && this.artifactId.equals(other.artifactId);
			return result;
		}

		@Override
		public int hashCode() {
			int result = this.groupId.hashCode();
			result = 31 * result + this.artifactId.hashCode();
			return result;
		}

		Map<String, String> asMap() {
			Map<String, String> map = new HashMap<>();
			map.put("group", this.groupId);
			map.put("module", this.artifactId);
			return Collections.unmodifiableMap(map);
		}

		@Override
		public String toString() {
			return getGroupAndArtifactId();
		}

		String getGroupAndArtifactId() {
			return this.groupId + ":" + this.artifactId;
		}

	}

}
