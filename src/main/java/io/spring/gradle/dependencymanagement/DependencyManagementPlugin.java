/*
 * Copyright 2014-2022 the original author or authors.
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

package io.spring.gradle.dependencymanagement;

import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension;
import io.spring.gradle.dependencymanagement.internal.bridge.InternalComponents;
import io.spring.gradle.dependencymanagement.maven.PomDependencyManagementConfigurer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.api.publish.maven.MavenPublication;
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin;

/**
 * Main class for the dependency management plugin.
 *
 * @author Andy Wilkinson
 */
public class DependencyManagementPlugin implements Plugin<Project> {

	@Override
	public void apply(Project project) {
		InternalComponents internalComponents = new InternalComponents(project);
		DependencyManagementExtension dependencyManagementExtension = internalComponents
			.getDependencyManagementExtension();
		project.getExtensions().add("dependencyManagement", dependencyManagementExtension);
		internalComponents.createDependencyManagementReportTask("dependencyManagement");
		project.getConfigurations().all(internalComponents.getImplicitDependencyManagementCollector());
		project.getConfigurations().all(internalComponents.getDependencyManagementApplier());
		configurePomCustomization(project, dependencyManagementExtension);
	}

	private void configurePomCustomization(Project project,
			DependencyManagementExtension dependencyManagementExtension) {
		PomDependencyManagementConfigurer pomConfigurer = dependencyManagementExtension.getPomConfigurer();
		project.getPlugins()
			.withType(MavenPublishPlugin.class,
					(mavenPublishPlugin) -> configurePublishingExtension(project, pomConfigurer));
	}

	private void configurePublishingExtension(Project project, PomDependencyManagementConfigurer extension) {
		project.getExtensions()
			.configure(PublishingExtension.class,
					(publishingExtension) -> configurePublications(publishingExtension, extension));
	}

	private void configurePublications(PublishingExtension publishingExtension,
			PomDependencyManagementConfigurer extension) {
		publishingExtension.getPublications()
			.withType(MavenPublication.class, (mavenPublication) -> mavenPublication.getPom().withXml(extension));
	}

}
