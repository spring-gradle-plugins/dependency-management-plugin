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

package io.spring.platform.gradle.dependencymanagement

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.mvn3.org.apache.maven.model.Repository
import org.gradle.mvn3.org.apache.maven.model.building.DefaultModelBuilderFactory
import org.gradle.mvn3.org.apache.maven.model.building.DefaultModelBuildingRequest
import org.gradle.mvn3.org.apache.maven.model.building.FileModelSource
import org.gradle.mvn3.org.apache.maven.model.building.ModelSource
import org.gradle.mvn3.org.apache.maven.model.resolution.InvalidRepositoryException
import org.gradle.mvn3.org.apache.maven.model.resolution.ModelResolver
import org.gradle.mvn3.org.apache.maven.model.resolution.UnresolvableModelException

class DependencyManagement {

	DependencyHandler dependencies

	ConfigurationContainer configurations

	Configuration configuration

	private boolean resolved

	private Map versions = [:]

	void apply(DependencyResolveDetails details) {
		resolveIfNecessary()
		def id = details.requested.group + ":" + details.requested.name
		def version = versions[id]
		if (version) {
			details.useVersion(version)
		}
	}

	void resolveIfNecessary() {
		if (!resolved) {
			resolve()
		}
		resolved = true
	}

	void resolve() {
		configuration.resolve().each { File file ->
			def modelBuilder = new DefaultModelBuilderFactory().newInstance()
			def request = new DefaultModelBuildingRequest()
			request.setModelSource(new FileModelSource(file))
			request.modelResolver = new StandardModelResolver()
			def result = modelBuilder.build(request)
			result.effectiveModel.dependencyManagement.dependencies.each { dependency ->
				versions[dependency.groupId + ":" + dependency.artifactId] = dependency.version
			}
		}
	}

	private class StandardModelResolver implements ModelResolver {

		@Override
		public ModelSource resolveModel(String groupId, String artifactId, String version)
				throws UnresolvableModelException {
			def dependency = dependencies.create("$groupId:$artifactId:$version@pom")
			def configuration = configurations.detachedConfiguration(dependency)
			new FileModelSource(configuration.resolve()[0])
		}

		@Override
		public void addRepository(Repository repository) {
		}

		@Override
		public ModelResolver newCopy() {
			this
		}
	}
}
