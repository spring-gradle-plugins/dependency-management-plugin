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
import io.spring.gradle.dependencymanagement.maven.EffectiveModelBuilder;
import io.spring.gradle.dependencymanagement.maven.ModelExclusionCollector;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.component.ProjectComponentIdentifier;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.artifacts.result.ResolvedComponentResult;
import org.gradle.api.specs.Specs;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the {@link Exclusions exclusions} for a {@link ResolvedComponentResult dependency}
 *
 * @author Andy Wilkinson
 */
public class ExclusionResolver {

    private final DependencyHandler dependencyHandler;

    private final DependencyManagementConfigurationContainer configurationContainer;

    private final EffectiveModelBuilder effectiveModelBuilder;

    private final Map<String, Exclusions> exclusionsCache = new HashMap<String, Exclusions>();

    public ExclusionResolver(DependencyHandler dependencyHandler,
            DependencyManagementConfigurationContainer configurationContainer,
            EffectiveModelBuilder effectiveModelBuilder) {
        this.dependencyHandler = dependencyHandler;
        this.configurationContainer = configurationContainer;
        this.effectiveModelBuilder = effectiveModelBuilder;
    }

    public Map<String, Exclusions> resolveExclusions(
            Collection<ResolvedComponentResult> resolvedComponents) {
        List<Dependency> dependencies = new ArrayList<Dependency>();
        Map<String, Exclusions> exclusionsById = new HashMap<String, Exclusions>();
        for (ResolvedComponentResult resolvedComponent : resolvedComponents) {
            if (!(resolvedComponent
                    .getId() instanceof ProjectComponentIdentifier) && resolvedComponent
                    .getModuleVersion().getGroup() != null && resolvedComponent.getModuleVersion()
                    .getName() != null) {
                String id = resolvedComponent.getModuleVersion()
                        .getGroup() + ":" + resolvedComponent.getModuleVersion().getName();
                Exclusions exclusions = this.exclusionsCache.get(id);
                if (exclusions != null) {
                    exclusionsById.put(id, exclusions);
                }
                else {
                    dependencies.add(this.dependencyHandler.create(id + ":" +
                            resolvedComponent.getModuleVersion().getVersion() + "@pom"));
                }
            }
        }
        Configuration configuration = this.configurationContainer.newConfiguration(dependencies
                        .toArray(new Dependency[dependencies.size()]));
        ModelExclusionCollector exclusionsCollector = new ModelExclusionCollector();
        for (ResolvedArtifact resolvedArtifact : configuration.getResolvedConfiguration()
                .getLenientConfiguration().getArtifacts(Specs.SATISFIES_ALL)) {
            ModuleVersionIdentifier moduleId = resolvedArtifact.getModuleVersion().getId();
            File pom = resolvedArtifact.getFile();
            Model model = this.effectiveModelBuilder.buildModel(pom);
            Exclusions exclusions = (Exclusions) exclusionsCollector.collectExclusions(model);
            String id = moduleId.getGroup() + ":" + moduleId.getName();
            this.exclusionsCache.put(id, exclusions);
            exclusionsById.put(id, exclusions);
        }
        return exclusionsById;
    }

}
