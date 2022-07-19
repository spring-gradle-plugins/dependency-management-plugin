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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;
import io.spring.gradle.dependencymanagement.internal.pom.Dependency;
import io.spring.gradle.dependencymanagement.internal.pom.Pom;
import io.spring.gradle.dependencymanagement.internal.pom.PomReference;
import io.spring.gradle.dependencymanagement.internal.pom.PomResolver;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;

/**
 * Resolves the {@link Exclusions exclusions} for a {@link ResolvedComponentResult
 * dependency}.
 *
 * @author Andy Wilkinson
 */
class ExclusionResolver {

	private static final Set<String> IGNORED_SCOPES = Collections
			.unmodifiableSet(new HashSet<>(Arrays.asList("provided", "test")));

	private final Map<String, Exclusions> exclusionsCache = new HashMap<>();

	private final PomResolver pomResolver;

	ExclusionResolver(PomResolver pomResolver) {
		this.pomResolver = pomResolver;
	}

	Map<String, Exclusions> resolveExclusions(Collection<ResolvedComponentResult> resolvedComponents) {
		List<PomReference> pomReferences = new ArrayList<>();
		Map<String, Exclusions> exclusionsById = new HashMap<>();
		for (ResolvedComponentResult resolvedComponent : resolvedComponents) {
			ModuleVersionIdentifier moduleVersion = resolvedComponent.getModuleVersion();
			if (!(resolvedComponent.getId() instanceof ProjectComponentIdentifier) && moduleVersion.getGroup() != null
					&& moduleVersion.getName() != null) {
				String id = moduleVersion.getGroup() + ":" + moduleVersion.getName();
				Exclusions exclusions = this.exclusionsCache.get(id);
				if (exclusions != null) {
					exclusionsById.put(id, exclusions);
				}
				else {
					pomReferences.add(new PomReference(new Coordinates(moduleVersion.getGroup(),
							moduleVersion.getName(), moduleVersion.getVersion())));
				}
			}
		}
		List<Pom> poms = this.pomResolver.resolvePomsLeniently(pomReferences);
		for (Pom pom : poms) {
			String id = pom.getCoordinates().getGroupAndArtifactId();
			Exclusions exclusions = collectExclusions(pom);
			this.exclusionsCache.put(id, exclusions);
			exclusionsById.put(id, exclusions);
		}
		return exclusionsById;
	}

	private Exclusions collectExclusions(Pom pom) {
		Exclusions exclusions = new Exclusions();
		List<Dependency> dependencies = new ArrayList<>(pom.getManagedDependencies());
		dependencies.addAll(pom.getDependencies());
		for (Dependency dependency : dependencies) {
			if (dependency.getExclusions() != null && !dependency.isOptional()
					&& !IGNORED_SCOPES.contains(dependency.getScope())) {
				String id = dependency.getCoordinates().getGroupAndArtifactId();
				exclusions.add(id, new HashSet<>(dependency.getExclusions()));
			}
		}
		return exclusions;
	}

}
