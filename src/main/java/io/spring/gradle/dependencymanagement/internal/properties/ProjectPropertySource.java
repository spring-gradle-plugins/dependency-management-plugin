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

package io.spring.gradle.dependencymanagement.internal.properties;

import org.gradle.api.Project;

/**
 * A {@link PropertySource} backed by a {@link Project}.
 *
 * @author Andy Wilkinson
 * @see Project#hasProperty(String)
 */
public class ProjectPropertySource implements PropertySource {

    private final Project project;

    /**
     * Creates a new {@link ProjectPropertySource} backed by the given {@code project}.
     *
     * @param project the project
     */
    public ProjectPropertySource(Project project) {
        this.project = project;
    }

    @Override
    public String getProperty(String name) {
        if (this.project.hasProperty(name) && this.project.property(name) != null) {
            return this.project.property(name).toString();
        }
        return null;
    }

}
