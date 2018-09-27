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

import groovy.lang.Closure;
import groovy.lang.GString;
import groovy.lang.GroovyObjectSupport;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;

import io.spring.gradle.dependencymanagement.dsl.ImportsHandler;
import io.spring.gradle.dependencymanagement.dsl.MavenBomHandler;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import io.spring.gradle.dependencymanagement.internal.pom.Coordinates;

/**
 * Standard implementation of {@link ImportsHandler}.
 *
 * @author Andy Wilkinson
 */
class StandardImportsHandler extends GroovyObjectSupport implements ImportsHandler {

    private final DependencyManagementContainer container;

    private final Configuration configuration;

    StandardImportsHandler(DependencyManagementContainer container, Configuration configuration) {
        this.container = container;
        this.configuration = configuration;
    }

    @Override
    public void mavenBom(Object dependencyNotation) {
        this.mavenBom(dependencyNotation, (Action<MavenBomHandler>) null);
    }

    @Override
    public void mavenBom(Object dependencyNotation, final Closure closure) {
        mavenBom(dependencyNotation, new Action<MavenBomHandler>() {

            @Override
            public void execute(MavenBomHandler mavenBomHandler) {
                if (closure != null) {
                    closure.setDelegate(mavenBomHandler);
                    closure.setResolveStrategy(Closure.DELEGATE_FIRST);
                    closure.call();
                }
            }
        });
    }

    @Override
    public void mavenBom(Object dependencyNotation, Action<MavenBomHandler> action) {
        StandardMavenBomHandler mavenBomHandler = new StandardMavenBomHandler();
        if (action != null) {
            action.execute(mavenBomHandler);
        }
        if (dependencyNotation instanceof String || dependencyNotation instanceof GString) {
            String[] components = dependencyNotation.toString().split(":");
            if (components.length != 3) {
                throw new IllegalArgumentException("Bom coordinates must be of the form groupId:artifactId:version");
            }
            dependencyNotation = new Coordinates(components[0], components[1], components[2]);
        }
        this.container.importBom(this.configuration, dependencyNotation, mavenBomHandler.getBomProperties());
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
