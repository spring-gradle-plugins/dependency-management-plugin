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

package io.spring.gradle.dependencymanagement.dsl;

/**
 * A handler for configuring the customization of generated POMs.
 *
 * @author Andy Wilkinson
 */
public interface GeneratedPomCustomizationHandler {

    /**
     * Configures how imported boms are included in generated pom.
     * <p>
     * A convenience method for {@link #includeImportedBomsBy(IncludeImportedBomAction)} that allows the action to be
     * expressed as a {@link String}. The action is resolved by calling the {@link
     * IncludeImportedBomAction#valueOf(String) valueOf(String)} method of {@code IncludeImportedBomAction} with the
     * {@link String#toUpperCase() upper-cased} string value.
     *
     * @param action the action used to include imported boms
     */
    void includeImportedBomsBy(String action);

    /**
     * Configures how imported boms are included in generated pom.
     *
     * @param action the action used to include imported boms
     */
    void includeImportedBomsBy(IncludeImportedBomAction action);

    /**
     * Sets whether or not customization of generated poms is enabled. Defaults to {@code true}.
     *
     * @param enabled whether or not customization is enabled
     */
    void setEnabled(boolean enabled);

    /**
     * Sets whether or not customization of generated poms is enabled. Defaults to {@code true}.
     *
     * @param enabled whether or not customization is enabled
     */
    void enabled(boolean enabled);

    /**
     * An enumeration of how an imported bom should be included in a generated pom.
     */
    enum IncludeImportedBomAction {

        /**
         * Include the bom by importing it into the generated pom.
         */
        IMPORTING,

        /**
         * Include the bom by copying it into the generated pom.
         */
        COPYING;

    }

}
