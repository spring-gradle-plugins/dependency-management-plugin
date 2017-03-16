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

package io.spring.gradle.dependencymanagement.internal.maven;

import io.spring.gradle.dependencymanagement.internal.properties.PropertySource;
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.AbstractValueSource;

/**
 * An {@link AbstractValueSource} backed by a {@link PropertySource}.
 *
 * @author Andy Wilkinson
 */
class PropertySourceValueSource extends AbstractValueSource {

    private final PropertySource propertySource;

    PropertySourceValueSource(PropertySource propertySource) {
        super(false);
        this.propertySource = propertySource;
    }

    @Override
    public Object getValue(String name) {
        return this.propertySource.getProperty(name);
    }

}
