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

import org.gradle.api.GradleException

public class DependenciesHandler {

	private final DependencyManagement dependencyManagement

	DependenciesHandler(DependencyManagement dependencyManagement) {
		this.dependencyManagement = dependencyManagement
	}

	def dependencySet(Map setSpecification, Closure closure) {
        def group = setSpecification['group']
        def version = setSpecification['version']

        if (hasText(group) && hasText(version)) {
            closure.setResolveStrategy(Closure.DELEGATE_ONLY)
            closure.delegate = new DependencySetHandler(group, version)
            closure.call()
        } else {
            throw new GradleException("A dependency set requires both a group and a version")
        }
	}

	def methodMissing(String name, args) {
		dependencyManagement.versions[name] = args[0]
	}

    def hasText(String string) {
        return string != null && string.trim().length() > 0
    }

    private class DependencySetHandler {

        private final String group

        private final String version

        DependencySetHandler(String group, String version) {
            this.group = group
            this.version = version
        }

        def entry(String entry) {
            String name = "$group:$entry"
            dependencyManagement.versions[name] = version
        }
    }
}
