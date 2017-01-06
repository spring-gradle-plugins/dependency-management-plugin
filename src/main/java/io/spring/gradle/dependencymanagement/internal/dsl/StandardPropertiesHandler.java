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

package io.spring.gradle.dependencymanagement.internal.dsl;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.gradle.api.InvalidUserDataException;
import org.gradle.api.artifacts.Configuration;

import io.spring.gradle.dependencymanagement.dsl.PropertiesHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;

/**
 * Standard implementation of {@link PropertiesHandler}.
 *
 * @author Julien Lafourcade
 */
public class StandardPropertiesHandler implements PropertiesHandler {

    private static final String KEY_NAME = "name";
    private static final String KEY_VALUE = "value";

    private final DependencyManagementContainer container;

    private final Configuration configuration;

    StandardPropertiesHandler(DependencyManagementContainer container, Configuration configuration) {
        this.container = container;
        this.configuration = configuration;
    }

    @Override
    public void property(String property) {
        String[] components = property.toString().split(":");
        if (components.length != 2) {
            throw new InvalidUserDataException("Property '" + property + "' is malformed. The required form is"
                    + " 'name:value'");
        }
        configureProperty(components[0], components[1]);
    }

    @Override
    public void property(Map<String, String> property) {
        Set<String> missingAttributes = new LinkedHashSet<String>(Arrays.asList(KEY_NAME, KEY_VALUE));
        missingAttributes.removeAll(property.keySet());
        if (!missingAttributes.isEmpty()) {
            throw new InvalidUserDataException("Property identifier '" + property + "' did not specify " +
                    toCommaSeparatedString(missingAttributes));
        }
        configureProperty(property.get(KEY_NAME), property.get(KEY_VALUE));

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

    private void configureProperty(String name, String value) {
        this.container.addProperty(this.configuration, name, value);
    }

}
