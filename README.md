# Dependency management plugin

A Gradle plugin that provides Maven-like dependency management

## Requirements

 - Gradle 2.0

## Using the plugin

The plugin isn't available in a repository, so you'll have to build it and install it into
your local Maven repository:

```
./gradlew build install
```

With that done, you can then use the plugin in your Gradle builds:

```
buildscript {
	repositories {
		mavenLocal()
	}
	dependencies {
		classpath 'io.spring.platform.gradle:dependency-management-plugin:0.1.0.BUILD-SNAPSHOT'
	}
}

repositories {
	mavenCentral()
}

apply plugin: 'dependency-management'
apply plugin: 'java'

dependencyManagement {
	imports {
		mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
	}
}

dependencies {
	compile 'org.springframework.integration:spring-integration-core'
}

```

The plugin will provide a version for the `spring-integration-core` dependency. It will also look
at every transitive dependency, and where dependency management for that dependency is provided, it
will update its version:

```
$ gradle dependencies --configuration compile
:dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compile - Compile classpath for source set 'main'.
\--- org.springframework.integration:spring-integration-core: -> 4.0.2.RELEASE
     +--- org.springframework.retry:spring-retry:1.1.0.RELEASE
     |    \--- org.springframework:spring-context:4.0.3.RELEASE -> 4.0.6.RELEASE
     |         +--- org.springframework:spring-aop:4.0.6.RELEASE
     |         |    +--- aopalliance:aopalliance:1.0
     |         |    +--- org.springframework:spring-beans:4.0.6.RELEASE
     |         |    |    \--- org.springframework:spring-core:4.0.6.RELEASE
     |         |    |         \--- commons-logging:commons-logging:1.1.3
     |         |    \--- org.springframework:spring-core:4.0.6.RELEASE (*)
     |         +--- org.springframework:spring-beans:4.0.6.RELEASE (*)
     |         +--- org.springframework:spring-core:4.0.6.RELEASE (*)
     |         \--- org.springframework:spring-expression:4.0.6.RELEASE
     |              \--- org.springframework:spring-core:4.0.6.RELEASE (*)
     +--- org.springframework:spring-tx:4.0.5.RELEASE -> 4.0.6.RELEASE
     |    +--- org.springframework:spring-beans:4.0.6.RELEASE (*)
     |    \--- org.springframework:spring-core:4.0.6.RELEASE (*)
     +--- org.springframework:spring-messaging:4.0.5.RELEASE -> 4.0.6.RELEASE
     |    +--- org.springframework:spring-beans:4.0.6.RELEASE (*)
     |    +--- org.springframework:spring-context:4.0.6.RELEASE (*)
     |    \--- org.springframework:spring-core:4.0.6.RELEASE (*)
     +--- org.springframework:spring-context:4.0.5.RELEASE -> 4.0.6.RELEASE (*)
     \--- org.springframework:spring-aop:4.0.5.RELEASE -> 4.0.6.RELEASE (*)
```

In the output above, you can see that the version of `spring-integration-core` has been set to
`4.0.2.RELEASE` and the versions of all of the Spring Framework dependencies have been changed to
`4.0.6.RELEASE`.

### Overridding versions in a bom

When the bom is being processed, Gradle's properties are used as a source during the property
resolution process. If the bom is written to use properties for its versions, this allows you to
override a version.

Building on the example above, the Spring IO Platform bom that is being used contains a property
named `spring.version` that's used to determine the version of Spring Framework. By default, its
value is `4.0.6.RELEASE`. This property can be overridden by configuring the `spring.version`
property via any of the mechanisms that Gradle provides. For example, you may choose to configure
it in your `build.gradle` script:

```
ext['spring.version'] = '4.0.4.RELEASE'
```

Or in `gradle.properties`

```
spring.version=4.0.4.RELEASE
```

However you configure it, the version of Spring Framework that's provided will now be
`4.0.4.RELEASE`:

```
gradle dependencies --configuration compile
:dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compile - Compile classpath for source set 'main'.
\--- org.springframework.integration:spring-integration-core: -> 4.0.2.RELEASE
     +--- org.springframework.retry:spring-retry:1.1.0.RELEASE
     |    \--- org.springframework:spring-context:4.0.3.RELEASE -> 4.0.4.RELEASE
     |         +--- org.springframework:spring-aop:4.0.4.RELEASE
     |         |    +--- aopalliance:aopalliance:1.0
     |         |    +--- org.springframework:spring-beans:4.0.4.RELEASE
     |         |    |    \--- org.springframework:spring-core:4.0.4.RELEASE
     |         |    |         \--- commons-logging:commons-logging:1.1.3
     |         |    \--- org.springframework:spring-core:4.0.4.RELEASE (*)
     |         +--- org.springframework:spring-beans:4.0.4.RELEASE (*)
     |         +--- org.springframework:spring-core:4.0.4.RELEASE (*)
     |         \--- org.springframework:spring-expression:4.0.4.RELEASE
     |              \--- org.springframework:spring-core:4.0.4.RELEASE (*)
     +--- org.springframework:spring-tx:4.0.5.RELEASE -> 4.0.4.RELEASE
     |    +--- org.springframework:spring-beans:4.0.4.RELEASE (*)
     |    \--- org.springframework:spring-core:4.0.4.RELEASE (*)
     +--- org.springframework:spring-messaging:4.0.5.RELEASE -> 4.0.4.RELEASE
     |    +--- org.springframework:spring-beans:4.0.4.RELEASE (*)
     |    +--- org.springframework:spring-context:4.0.4.RELEASE (*)
     |    \--- org.springframework:spring-core:4.0.4.RELEASE (*)
     +--- org.springframework:spring-context:4.0.5.RELEASE -> 4.0.4.RELEASE (*)
     \--- org.springframework:spring-aop:4.0.5.RELEASE -> 4.0.4.RELEASE (*)
```