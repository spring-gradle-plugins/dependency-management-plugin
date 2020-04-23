/*
 * Copyright 2014-2016 the original author or authors.
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.result.ResolvedComponentResult;

import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;
import io.spring.gradle.dependencymanagement.internal.pom.Dependency;
import io.spring.gradle.dependencymanagement.internal.pom.Pom;
import io.spring.gradle.dependencymanagement.internal.pom.PomReference;
import io.spring.gradle.dependencymanagement.internal.pom.PomResolver;

/**
 * Resolves the {@link Exclusions exclusions} for a {@link ResolvedComponentResult dependency}.
 *
 * @author Andy Wilkinson
 */
class ExclusionResolver {

    private static final Set<String> IGNORED_SCOPES = Collections
            .unmodifiableSet(new HashSet<String>(Arrays.asList("provided", "test")));

    // Least Recently Used Cache of exclusions by POM coordinates.
    // This improves the performance of dependency resolution when using the Gradle daemon,
    // avoiding reparsing the same POM files on each resolve.
    private static final Map<String, Exclusions> exclusionsByPomCache = Collections.synchronizedMap(
            new LinkedHashMap<String, Exclusions>(16, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry eldest) {
            // Equates to ~5MB if each entry is ~0.5KB
            return size() > 10000;
        }
    });

    private final PomResolver pomResolver;

    ExclusionResolver(PomResolver pomResolver) {
        this.pomResolver = pomResolver;
    }

    Map<String, Exclusions> resolveExclusions(
            Collection<ResolvedComponentResult> resolvedComponents) {
        List<PomReference> pomReferences = new ArrayList<PomReference>();
        Map<String, Exclusions> exclusionsById = new HashMap<String, Exclusions>();
        for (ResolvedComponentResult resolvedComponent : resolvedComponents) {
            if (!(resolvedComponent
                    .getId() instanceof ProjectComponentIdentifier) && resolvedComponent
                    .getModuleVersion().getGroup() != null && resolvedComponent.getModuleVersion()
                    .getName() != null) {
                String id = resolvedComponent.getModuleVersion()
                        .getGroup() + ":" + resolvedComponent.getModuleVersion().getName();
                String coordinates = id + ":" + resolvedComponent.getModuleVersion().getVersion();
                Exclusions exclusions = exclusionsByPomCache.get(coordinates);
                if (exclusions != null) {
                    exclusionsById.put(id, exclusions);
                }
                else {
                    pomReferences.add(new PomReference(new Coordinates(resolvedComponent.getModuleVersion().getGroup(),
                            resolvedComponent.getModuleVersion().getName(),
                            resolvedComponent.getModuleVersion().getVersion())));
                }
            }
        }
        List<Pom> poms = this.pomResolver.resolvePomsLeniently(pomReferences);
        for (Pom pom: poms) {
            String id = pom.getCoordinates().getGroupId() + ":" + pom.getCoordinates().getArtifactId();
            Exclusions exclusions = collectExclusions(pom);
            exclusionsById.put(id, exclusions);

            String coordinates = id + ":" + pom.getCoordinates().getVersion();
            exclusionsByPomCache.put(coordinates, exclusions);
        }
        return exclusionsById;
    }

    private Exclusions collectExclusions(Pom pom) {
        Exclusions exclusions = new Exclusions();
        List<Dependency> dependencies = new ArrayList<Dependency>(pom.getManagedDependencies());
        dependencies.addAll(pom.getDependencies());
        for (Dependency dependency : dependencies) {
            if (dependency.getExclusions() != null && !dependency.isOptional() && !IGNORED_SCOPES
                    .contains(dependency.getScope())) {
                String dependencyId = dependency.getCoordinates().getGroupId() + ":" +
                        dependency.getCoordinates().getArtifactId();
                exclusions.add(dependencyId, new HashSet<String>(dependency.getExclusions()));
            }

        }
        return exclusions;
    }

}
