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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.spring.gradle.dependencymanagement.org.apache.maven.model.Model;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelBuildingRequest;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.building.ModelProblemCollector;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.interpolation.ModelInterpolator;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.interpolation.StringSearchModelInterpolator;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.path.DefaultPathTranslator;
import io.spring.gradle.dependencymanagement.org.apache.maven.model.path.DefaultUrlNormalizer;
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.MapBasedValueSource;
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import io.spring.gradle.dependencymanagement.org.codehaus.plexus.interpolation.ValueSource;

/**
 * A {@link ModelInterpolator} that uses a various properties as {@link ValueSource ValueSources}.
 *
 * @author Andy Wilkinson
 */
class PropertiesModelInterpolator extends StringSearchModelInterpolator {

    private final Map<String, ?> properties;

    PropertiesModelInterpolator(Map<String, ?> properties) {
        this.properties = properties;
        setUrlNormalizer(new DefaultUrlNormalizer());
        setPathTranslator(new DefaultPathTranslator());
    }

    @Override
    public List<ValueSource> createValueSources(Model model, File projectDir,
            ModelBuildingRequest request, ModelProblemCollector collector) {
        List<ValueSource> valueSources = new ArrayList<ValueSource>(
                Arrays.asList(new MapBasedValueSource(this.properties),
                        new PropertiesBasedValueSource(System.getProperties())));
        valueSources.addAll(super.createValueSources(model, projectDir, request, collector));
        return valueSources;
    }


}
