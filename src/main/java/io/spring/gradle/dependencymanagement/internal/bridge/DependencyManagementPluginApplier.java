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

package io.spring.gradle.dependencymanagement.internal.bridge;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.maven.MavenDeployer;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;
import org.gradle.api.tasks.Upload;

import io.spring.gradle.dependencymanagement.internal.DependencyManagementSettings;
import io.spring.gradle.dependencymanagement.PomDependencyManagementConfigurer;
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementApplier;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementConfigurationContainer;
import io.spring.gradle.dependencymanagement.internal.DependencyManagementContainer;
import io.spring.gradle.dependencymanagement.internal.ImplicitDependencyManagementCollector;
import io.spring.gradle.dependencymanagement.internal.dsl.StandardDependencyManagementExtension;
import io.spring.gradle.dependencymanagement.internal.maven.MavenPomResolver;
import io.spring.gradle.dependencymanagement.internal.report.DependencyManagementReportTask;

/**
 * Applies the plugin to a project, providing isolation between the plugin's API and its internal classes.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementPluginApplier {

    /**
     * Applies the plugin to the given {@code project}.
     *
     * @param project the project
     */
    public void apply(final Project project) {
        DependencyManagementConfigurationContainer configurationContainer =
                new DependencyManagementConfigurationContainer(project);

        MavenPomResolver pomResolver = new MavenPomResolver(project, configurationContainer);
        final DependencyManagementContainer dependencyManagementContainer =
                new DependencyManagementContainer(project, pomResolver);

        DependencyManagementSettings dependencyManagementSettings = new DependencyManagementSettings();

        final DependencyManagementExtension dependencyManagementExtension =
                new StandardDependencyManagementExtension(dependencyManagementContainer, configurationContainer,
                        project, dependencyManagementSettings);

        ImplicitDependencyManagementCollector implicitDependencyManagementCollector =
                new ImplicitDependencyManagementCollector(dependencyManagementContainer, dependencyManagementSettings);

        DependencyManagementApplier dependencyManagementApplier = new DependencyManagementApplier(project,
                dependencyManagementContainer, configurationContainer, dependencyManagementSettings, pomResolver);

        project.getExtensions().add("dependencyManagement", dependencyManagementExtension);

        project.getTasks().create("dependencyManagement", DependencyManagementReportTask.class,
                new Action<DependencyManagementReportTask>() {

                    @Override
                    public void execute(DependencyManagementReportTask dependencyManagementReportTask) {
                        dependencyManagementReportTask.setDependencyManagementContainer(dependencyManagementContainer);
                    }

                });

        project.getConfigurations().all(implicitDependencyManagementCollector);
        project.getConfigurations().all(dependencyManagementApplier);
        project.getTasks().withType(Upload.class, new Action<Upload>() {
            @Override
            public void execute(Upload upload) {
                upload.getRepositories().withType(MavenDeployer.class, new Action<MavenDeployer>() {
                    @Override
                    public void execute(MavenDeployer mavenDeployer) {
                        mavenDeployer.getPom().withXml(dependencyManagementExtension.getPomConfigurer());
                    }
                });

            }

        });
        project.getPlugins().withType(MavenPublishPlugin.class, new Action<MavenPublishPlugin>() {
            @Override
            public void execute(MavenPublishPlugin mavenPublishPlugin) {
                configurePublishingExtension(project, dependencyManagementExtension.getPomConfigurer());
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
