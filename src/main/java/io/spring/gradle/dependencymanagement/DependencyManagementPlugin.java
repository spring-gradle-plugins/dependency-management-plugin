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

package io.spring.gradle.dependencymanagement;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.maven.MavenDeployer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.Upload;

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.internal.bridge.InternalComponents;
import io.spring.gradle.dependencymanagement.maven.PomDependencyManagementConfigurer;

/**
 * Main class for the dependency management plugin.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementPlugin implements Plugin<Project> {

    @Override
    public void apply(final Project project) {
        InternalComponents internalComponents = new InternalComponents(project);

        DependencyManagementExtension dependencyManagementExtension =
                internalComponents.getDependencyManagementExtension();

        project.getExtensions().add("dependencyManagement", dependencyManagementExtension);
        internalComponents.createDependencyManagementReportTask("dependencyManagement");

        project.getConfigurations().all(internalComponents.getImplicitDependencyManagementCollector());
        project.getConfigurations().all(internalComponents.getDependencyManagementApplier());

        configurePomCustomization(project, dependencyManagementExtension);
    }

    private void configurePomCustomization(final Project project, DependencyManagementExtension dependencyManagementExtension) {
        final PomDependencyManagementConfigurer pomConfigurer = dependencyManagementExtension.getPomConfigurer();
        project.getTasks().withType(Upload.class, new Action<Upload>() {
            @Override
            public void execute(Upload upload) {
                upload.getRepositories().withType(MavenDeployer.class, new Action<MavenDeployer>() {

                    @Override
                    public void execute(MavenDeployer mavenDeployer) {
                        mavenDeployer.getPom().withXml(pomConfigurer);
                    }

                });

            }

        });
        project.getPlugins().withType(MavenPublishPlugin.class, new Action<MavenPublishPlugin>() {

            @Override
            public void execute(MavenPublishPlugin mavenPublishPlugin) {
                configurePublishingExtension(project, pomConfigurer);
            }

        });
    }

    private void configurePublishingExtension(Project project, final PomDependencyManagementConfigurer extension) {
        project.getExtensions().configure(PublishingExtension.class, new Action<PublishingExtension>() {

            @Override
            public void execute(PublishingExtension publishingExtension) {
                configurePublications(publishingExtension, extension);
            }

        });
    }

    private void configurePublications(PublishingExtension publishingExtension,
            final PomDependencyManagementConfigurer extension) {
        publishingExtension.getPublications().withType(MavenPublication.class, new Action<MavenPublication>() {

            @Override
            public void execute(MavenPublication mavenPublication) {
                mavenPublication.getPom().withXml(extension);
            }

        });
    }

}
