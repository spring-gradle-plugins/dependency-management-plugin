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

import java.util.Arrays;
import java.util.List;

/**
 * A {@link PropertySource} that delegates to other property sources.
 *
 * @author Andy Wilkinson
 */
public class CompositePropertySource implements PropertySource {

    private final List<PropertySource> delegates;

    /**
     * Creates a new {@code CompositePropertySource} that will delegate to the given {@code delegates}.
     *
     * @param delegates the delegates
     */
    public CompositePropertySource(PropertySource... delegates) {
        this.delegates = Arrays.asList(delegates);
    }

    @Override
    public Object getProperty(String name) {
        for (PropertySource delegate: this.delegates) {
            Object property = delegate.getProperty(name);
            if (property != null) {
                return property;
            }
        }
        return null;
    }

}
