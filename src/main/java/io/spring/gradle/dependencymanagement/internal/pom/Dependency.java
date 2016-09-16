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

package io.spring.gradle.dependencymanagement.internal.pom;

import java.util.Collections;
import java.util.Set;

/**
 * A dependency in a Maven {@link Pom}.
 *
 * @author Andy Wilkinson
 */
public final class Dependency {

    private final Coordinates coordinates;

    private final boolean optional;

    private final String type;

    private final String classifier;

    private final String scope;

    private final Set<String> exclusions;

    /**
     * Creates a new dependency.
     *
     * @param coordinates the coordinates
     * @param exclusions the exclusions int the form {@code groupId:artifactId}
     */
    public Dependency(Coordinates coordinates, Set<String> exclusions) {
        this(coordinates, false, null, null, null, exclusions);
    }

    /**
     * Creates a new dependency.
     *
     * @param coordinates the coordinates
     * @param optional whether the dependency is optional
     * @param type the type of the dependency
     * @param classifier the classifier of the dependency
     * @param scope the scope of the dependency
     * @param exclusions the exclusions int the form {@code groupId:artifactId}
     */
    public Dependency(Coordinates coordinates, boolean optional, String type,
            String classifier, String scope, Set<String> exclusions) {
        this.coordinates = coordinates;
        this.optional = optional;
        this.type = type;
        this.classifier = classifier;
        this.scope = scope;
        this.exclusions = exclusions == null ? Collections.<String>emptySet() : exclusions;
    }

    /**
     * Returns the coordinates of the dependency.
     *
     * @return the coordinates
     */
    public Coordinates getCoordinates() {
        return this.coordinates;
    }

    /**
     * Returns the exclusions of the dependency.
     *
     * @return the exclusions
     */
    public Set<String> getExclusions() {
        return this.exclusions;
    }

    /**
     * Returns the type of the dependency.
     *
     * @return the type
     */
    public String getType() {
        return this.type;
    }

    /**
     * Returns the classifier of the dependency.
     *
     * @return the classifier
     */
    public String getClassifier() {
        return this.classifier;
    }

    /**
     * Returns the scope of the dependency.
     *
     * @return the scope
     */
    public String getScope() {
        return this.scope;
    }

    /**
     * Returns whether or not the dependency is optional.
     *
     * @return {@code true} if it is optional, otherwise {@code false}
     */
    public boolean isOptional() {
        return this.optional;
    }

}
