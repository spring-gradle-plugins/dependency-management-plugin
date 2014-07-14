/*
 * Copyright 2014 the original author or authors.
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

package io.spring.gradle.dependencymanagement

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.mvn3.org.apache.maven.model.Model
import org.gradle.mvn3.org.apache.maven.model.Repository
import org.gradle.mvn3.org.apache.maven.model.building.DefaultModelBuilderFactory
import org.gradle.mvn3.org.apache.maven.model.building.DefaultModelBuildingRequest
import org.gradle.mvn3.org.apache.maven.model.building.FileModelSource
import org.gradle.mvn3.org.apache.maven.model.building.ModelBuildingRequest
import org.gradle.mvn3.org.apache.maven.model.building.ModelProblemCollector
import org.gradle.mvn3.org.apache.maven.model.building.ModelSource
import org.gradle.mvn3.org.apache.maven.model.interpolation.StringSearchModelInterpolator
import org.gradle.mvn3.org.apache.maven.model.resolution.ModelResolver
import org.gradle.mvn3.org.apache.maven.model.resolution.UnresolvableModelException
import org.gradle.mvn3.org.codehaus.plexus.interpolation.MapBasedValueSource
import org.gradle.mvn3.org.codehaus.plexus.interpolation.ValueSource

class DependencyManagement {

	private final Project project

	private final Configuration configuration

	private final DependencyManagement delegate

	private boolean resolved

	Map versions = [:]

	def DependencyManagement(Project project, DependencyManagement delegate) {
		this.project = project
		this.delegate = delegate
		this.configuration = this.project.configurations.detachedConfiguration()
	}

	void importBom(bomCoordinates) {
		configuration.dependencies.add(project.dependencies.create(bomCoordinates + '@pom'))
	}

	void apply(DependencyResolveDetails details) {
		resolveIfNecessary()
		def id = details.requested.group + ":" + details.requested.name
		def version = versions[id]
		if (version) {
			details.useVersion(version)
		} else if (delegate) {
			delegate.apply(details)
		}
	}

	void resolveIfNecessary() {
		if (!resolved) {
			resolve()
		}
		resolved = true
	}

	void resolve() {
		def existingVersions = [:]
		existingVersions << versions

		def modelBuilder = new DefaultModelBuilderFactory().newInstance()
		modelBuilder.modelInterpolator = new ProjectPropertiesModelInterpolator(project)

		configuration.resolve().each { File file ->
			def request = new DefaultModelBuildingRequest()
			request.setModelSource(new FileModelSource(file))
			request.modelResolver = new StandardModelResolver()
			def result = modelBuilder.build(request)
			result.effectiveModel.dependencyManagement.dependencies.each { dependency ->
				versions["$dependency.groupId:$dependency.artifactId" as String] = dependency.version
			}
		}

		versions << existingVersions
	}

	private static class ProjectPropertiesModelInterpolator extends StringSearchModelInterpolator {

		private final Project project

		ProjectPropertiesModelInterpolator(Project project) {
			this.project = project
		}

		List<ValueSource> createValueSources(Model model, File projectDir, ModelBuildingRequest request, ModelProblemCollector collector) {
			List valueSources = [new MapBasedValueSource(project.properties)]
			valueSources.addAll(super.createValueSources(model, projectDir, request, collector))
			valueSources
		}
	}

	private class StandardModelResolver implements ModelResolver {

		@Override
		ModelSource resolveModel(String groupId, String artifactId, String version)
				throws UnresolvableModelException {
			def dependency = project.dependencies.create("$groupId:$artifactId:$version@pom")
			def configuration = project.configurations.detachedConfiguration(dependency)
			new FileModelSource(configuration.resolve()[0])
		}

		@Override
		void addRepository(Repository repository) {
		}

		@Override
		ModelResolver newCopy() {
			this
		}
	}
}
