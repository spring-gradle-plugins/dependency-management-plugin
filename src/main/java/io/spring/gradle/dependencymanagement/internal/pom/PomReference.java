/*
 * Copyright 2014-2017 the original author or authors.
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

import io.spring.gradle.dependencymanagement.internal.properties.MapPropertySource;
import io.spring.gradle.dependencymanagement.internal.properties.PropertySource;

/**
 * A reference to a Maven pom.
 *
 * @author Andy Wilkinson
 */
public final class PomReference {

    private final Coordinates coordinates;

    private final PropertySource properties;

    /**
     * Creates a new {@code PomReference}.
     *
     * @param coordinates the coordinate of the referenced pom
     */
    public PomReference(Coordinates coordinates) {
        this(coordinates, new MapPropertySource(Collections.<String, Object>emptyMap()));
    }

    /**
     * Creates a new {@code PomReference}.
     *
     * @param coordinates the coordinates of the referenced pom
     * @param properties the properties that should be used when resolving the pom's contents
     */
    public PomReference(Coordinates coordinates, PropertySource properties) {
        this.coordinates = coordinates;
        this.properties = properties;
    }

    /**
     * Returns the coordinates of the referenced pom.
     *
     * @return the coordinates
     */
    public Coordinates getCoordinates() {
        return this.coordinates;
    }

    /**
     * Returns the properties that should be used when resolving the pom's contents.
     *
     * @return the properties
     */
    public PropertySource getProperties() {
        return this.properties;
    }

}
