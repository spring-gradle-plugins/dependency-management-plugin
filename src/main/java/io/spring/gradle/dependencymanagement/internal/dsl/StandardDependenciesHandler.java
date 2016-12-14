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

package io.spring.gradle.dependencymanagement.internal.dsl;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import io.spring.gradle.dependencymanagement.dsl.DependenciesHandler;
import io.spring.gradle.dependencymanagement.dsl.DependencyHandler;
import io.spring.gradle.dependencymanagement.dsl.DependencySetHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;

/**
 * Standard implementation of {@link DependenciesHandler}.
 *
 * @author Andy Wilkinson
 */
class StandardDependenciesHandler implements DependenciesHandler {

    private static final String KEY_GROUP = "group";

    private static final String KEY_NAME = "name";

    private static final String KEY_VERSION = "version";

    private final DependencyManagementContainer container;

    private final Configuration configuration;

    StandardDependenciesHandler(DependencyManagementContainer container, Configuration configuration) {
        this.container = container;
        this.configuration = configuration;
    }

    @Override
    public void dependencySet(Map<String, String> setSpecification, final Closure closure) {
        dependencySet(setSpecification, new Action<DependencySetHandler>() {

            @Override
            public void execute(DependencySetHandler dependencySetHandler) {
                closure.setResolveStrategy(Closure.DELEGATE_FIRST);
                closure.setDelegate(dependencySetHandler);
                closure.call();
            }

        });
    }

    @Override
    public void dependencySet(Map<String, String> setSpecification, Action<DependencySetHandler> action) {
        String group = setSpecification.get(KEY_GROUP);
        String version = setSpecification.get(KEY_VERSION);

        if (hasText(group) && hasText(version)) {
            action.execute(new StandardDependencySetHandler(group, version, this.container, this.configuration));
        }
        else {
            throw new GradleException("A dependency set requires both a group and a version");
        }
    }

    private boolean hasText(String string) {
        return string != null && string.trim().length() > 0;
    }

    @Override
    public void dependency(Object id) {
        dependency(id, (Action<DependencyHandler>) null);
    }

    @Override
    public void dependency(Object id, final Closure closure) {
        dependency(id, new Action<DependencyHandler>() {

            @Override
            public void execute(DependencyHandler dependencyHandler) {
                closure.setDelegate(dependencyHandler);
                closure.call();
            }

        });
    }

    @Override
    public void dependency(Object id, Action<DependencyHandler> action) {
        if (id instanceof CharSequence) {
            String[] components = id.toString().split(":");
            if (components.length == 3) {
                configureDependency(components[0], components[1], components[2], action);
            }
            else {
                throw new InvalidUserDataException("Dependency identifier '" + id + "' is malformed. The required form"
                        +  " is 'group:name:version'");
            }
        }
        else {
            @SuppressWarnings("unchecked")
            Map<String, Object> idMap = (Map<String, Object>) id;
            Set<String> missingAttributes = new LinkedHashSet<String>(Arrays.asList(KEY_GROUP, KEY_NAME, KEY_VERSION));
            missingAttributes.removeAll(idMap.keySet());
            if (!missingAttributes.isEmpty()) {
                throw new InvalidUserDataException("Dependency identifier '" + id + "' did not specify " +
                        toCommaSeparatedString(missingAttributes));
            }
            else {
                configureDependency(idMap.get(KEY_GROUP).toString(), idMap.get(KEY_NAME).toString(),
                        idMap.get(KEY_VERSION).toString(), action);
            }
        }
    }

    private String toCommaSeparatedString(Collection<String> items) {
        StringBuilder output = new StringBuilder();
        for (String item: items) {
            if (output.length() > 0) {
                output.append(", ");
            }
            output.append(item);
        }
        return output.toString();
    }

    private void configureDependency(String group, String name, String version, Action<DependencyHandler> action) {
        StandardDependencyHandler dependencyHandler = new StandardDependencyHandler();
        if (action != null) {
            action.execute(dependencyHandler);
        }
        this.container.addManagedVersion(this.configuration, group, name, version, dependencyHandler.getExclusions());
    }

    /**
     * Handlers missing properties by returning the {@link Project} property with the given {@code name}.
     *
     * @param name the name of the property
     * @return the value of the project property
     */
    public Object propertyMissing(String name) {
        return this.container.getProject().property(name);
    }

}
