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

package io.spring.gradle.dependencymanagement.internal.dsl

import spock.lang.Specification

/**
 * Tests for {@link StandardMavenBomHandler}.
 *
 * @author Andy Wilkinson
 */
class StandardMavenBomHandlerSpec extends Specification {

    StandardMavenBomHandler handler = new StandardMavenBomHandler()

    def "Bom properties can be configured using a map containing GStrings"() {
        when:
        def name = "foo.version"
        def version = "1.0"
        Map properties = ["$name":"$version"]
        this.handler.bomProperties(properties)

        then:
        with(this.handler.bomProperties) {
            it.getProperty('foo.version') instanceof String
            it.getProperty('foo.version') == '1.0'
        }
    }

}
