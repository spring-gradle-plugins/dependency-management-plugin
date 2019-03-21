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

package io.spring.gradle.dependencymanagement.dsl;

import java.util.Map;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolutionStrategy;

import io.spring.gradle.dependencymanagement.maven.PomDependencyManagementConfigurer;

/**
 * Extension that provides the entry point to the dependency management plugin's DSL.
 *
 * @author Andy Wilkinson
 */
public interface DependencyManagementExtension extends DependencyManagementHandler {

    /**
     * Configures the resolution strategy of all dependency management-related {@link Configuration Configurations}
     * using the given {@code closure}.
     *
     * @param closure the closure that will configure the resolution strategies
     */
    void resolutionStrategy(Closure closure);

    /**
     * Configures the resolution strategy of all dependency management-related {@link Configuration Configurations}
     * using the given {@code action}.
     *
     * @param action the action that will configure the resolution strategies
     */
    void resolutionStrategy(Action<ResolutionStrategy> action);

    /**
     * Uses the given {@code closure} to configure the customization of generated poms. The closure is called with
     * a {@link GeneratedPomCustomizationHandler} as its delegate.
     *
     * @param closure the closure
     * @see GeneratedPomCustomizationHandler
     */
    void generatedPomCustomization(Closure closure);

    /**
     * Uses the given {@code action} to configure the customization of generated poms.
     *
     * @param action the action
     * @see GeneratedPomCustomizationHandler
     */
    void generatedPomCustomization(Action<GeneratedPomCustomizationHandler> action);

    /**
     * Provides access to the {@link PomDependencyManagementConfigurer} that can be used to configure dependency
     * management in a generated pom.
     *
     * @return the pom configurer
     */
    PomDependencyManagementConfigurer getPomConfigurer();

    /**
     * Set whether or not Maven-style exclusions should be applied during dependency resolutions.
     * The default is {@code true}.
     *
     * @param applyMavenExclusions {@code true} if Maven-style exclusions should be applied, otherwise {@code false}
     */
    void setApplyMavenExclusions(boolean applyMavenExclusions);

    /**
     * Set whether or not Maven-style exclusions should be applied during dependency resolutions.
     * The default is {@code true}.
     *
     * @param applyMavenExclusions {@code true} if Maven-style exclusions should be applied, otherwise {@code false}
     */
    void applyMavenExclusions(boolean applyMavenExclusions);

    /**
     * Set whether dependency management should be overridden by versions declared on a project's dependencies. The
     * default is {@code true}.
     *
     * @param overriddenByDependencies {@code true} if dependency management should be overridden by dependencies'
     * versions, otherwise {@code false}
     */
    void setOverriddenByDependencies(boolean overriddenByDependencies);

    /**
     * Set whether dependency management should be overridden by versions declared on a project's dependencies. The
     * default is {@code true}.
     *
     * @param overriddenByDependencies {@code true} if dependency management should be overridden by dependencies'
     * versions, otherwise {@code false}
     */
    void overriddenByDependencies(boolean overriddenByDependencies);

    /**
     * Returns a map of the managed versions for a specific {@link Configuration}, ignoring its hierarchy. The key-value
     * pairs in the map have the form {@code group:name = version}.
     *
     * @param configuration the configuration
     * @return the managed versions for the configuration
     */
    Map<String, String> getManagedVersionsForConfiguration(Configuration configuration);

    /**
     * Returns a map of the managed versions for a specific {@link Configuration}, including its hierarchy. The key-value
     * pairs in the map have the form {@code group:name = version}.
     *
     * @param configuration the configuration
     * @return the managed versions for the configuration hierarchy
     */
    Map<String, String> getManagedVersionsForConfigurationHierarchy(Configuration configuration);

}
