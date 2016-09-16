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

package io.spring.gradle.dependencymanagement;

/**
 * Settings that control dependency management behaviour.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementSettings {

    private boolean applyMavenExclusions = true;

    private boolean overriddenByDependencies = true;

    private final PomCustomizationSettings pomCustomizationSettings = new PomCustomizationSettings();

    /**
     * Whether or not Maven-style exclusions should be applied during dependency resolution.
     *
     * @return {@code true} if Maven-style exclusions should be applied, otherwise {@code false}
     */
    public boolean isApplyMavenExclusions() {
        return this.applyMavenExclusions;
    }

    /**
     * Set whether or not Maven-style exclusions should be applied during dependency resolution.
     * The default is {@code true}.
     *
     * @param applyMavenExclusions {@code true} if Maven-style exclusions should be applied, otherwise {@code false}
     */
    public void setApplyMavenExclusions(boolean applyMavenExclusions) {
        this.applyMavenExclusions = applyMavenExclusions;
    }

    /**
     * Whether or not dependency management should be overridden by versions declared on a project's dependencies.
     *
     * @return {@code true} if dependency management should be overridden by dependencies' versions, otherwise {@code
     * false}
     */
    public boolean isOverriddenByDependencies() {
        return this.overriddenByDependencies;
    }

    /**
     * Set whether dependency management should be overridden by versions declared on a project's dependencies. The
     * default is {@code true}.
     *
     * @param overriddenByDependencies {@code true} if dependency management should be overridden by dependencies'
     * versions, otherwise {@code false}
     */
    public void setOverriddenByDependencies(boolean overriddenByDependencies) {
        this.overriddenByDependencies = overriddenByDependencies;
    }

    /**
     * Returns the settings for pom customization.
     *
     * @return the pom customizations settings
     */
    public PomCustomizationSettings getPomCustomizationSettings() {
        return this.pomCustomizationSettings;
    }

    /**
     * Settings for the plugin's customization of generated poms.
     */
    public static final class PomCustomizationSettings {

        private boolean enabled = true;

        private ImportedBomAction importedBomAction = ImportedBomAction.IMPORT;

        /**
         * Set the action that used to configure how an imported bom is applied to a generated pom. The default is
         * {@code import}. The alternative is {@code copy}.
         *
         * @param importedBomAction the action to apply
         */
        public void setImportedBomAction(String importedBomAction) {
            this.importedBomAction = ImportedBomAction.valueOf(importedBomAction.toUpperCase());
        }

        /**
         * Whether or not pom customization is enabled.
         *
         * @return {@code true} if it is enabled, {@code false} if it is not enabled
         */
        public boolean isEnabled() {
            return this.enabled;
        }

        /**
         * Sets whether or not pom customization is enabled. The default is {@code true}.
         *
         * @param enabled {@code true} if it is enabled, {@code false} if it is not enabled
         */
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        /**
         * Returns the action that's used to apply an imported bom to a generated pom.
         *
         * @return the action
         */
        public ImportedBomAction getImportedBomAction() {
            return this.importedBomAction;
        }

        /**
         * An enumeration of how an imported bom should be applied to a generated pom.
         */
        public enum ImportedBomAction {

            /**
             * Import the bom into the generated pom.
             */
            IMPORT,

            /**
             * Copy the bom's dependency management into the generated pom.
             */
            COPY;

        }

    }

}
