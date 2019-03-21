/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.spring.gradle.dependencymanagement.maven;

import groovy.util.Node;
import org.gradle.api.Action;
import org.gradle.api.XmlProvider;

/**
 * A {@code PomDependencyManagementConfigurer} is used to configure the dependency management of a Gradle-generated
 * Maven pom.
 *
 * @author Andy Wilkinson
 */
public interface PomDependencyManagementConfigurer extends Action<XmlProvider> {

    /**
     * Configures the dependency management of the pom that's available from the given {@code xmlProvider}.
     *
     * @param xmlProvider the provider of the pom's XML
     */
    @Override
    void execute(XmlProvider xmlProvider);

    /**
     * Configures the dependency management of the given {@code pom}.
     *
     * @param pom the pom to configure
     */
    void configurePom(Node pom);

}
