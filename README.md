# Dependency management plugin

A Gradle plugin that provides Maven-like dependency management. Based on the configured dependency
management metadata, the plugin will control the versions of your project's direct and transitive
dependencies.

## Requirements

 - Gradle 1.x or 2.x (the plugin is tested against 1.12 and 2.0)

## Using the plugin

The plugin is available from http://repo.spring.io where you can see [all the available versions]
[1]. The latest release is `0.1.0.RELEASE`. You can use it as follows:

```
buildscript {
	repositories {
		maven { url 'http://repo.spring.io/plugins-snapshot'}
	}
	dependencies {
		classpath 'io.spring.gradle:dependency-management-plugin:0.1.0.RELEASE'
	}
}

apply plugin: 'io.spring.dependency-management'

```

With this basic configuration in place, you're ready to configure the project's dependency
management and declare its dependencies.

## Dependency management configuration

You have two options for configuring the plugin's dependency management: use the plugin's
DSL to configure dependency management directly, or you can import one or more existing Maven boms.
Dependency management can be applied to every configuration (the default) or to one or more specific
configurations.

### Dependency management DSL

The DSL allows you to declare dependency management in the form `'groupId:artifactId' 'version'`.
For example:

```groovy
dependencyManagement {
     dependencies {
          'org.springframework:spring-core' '4.0.3.RELEASE'
          'commons-logging:commons-logging' '1.1.2'
     }
}

dependencies {
     compile 'org.springframework:spring-core'
}
```

This configuration will cause all dependencies (direct or transitive) on `spring-core` and
`commons-logging` to have the versions `4.0.3.RELEASE` and `1.1.2` respectively:

```
$ gradle dependencies --configuration compile
:dependencies

------------------------------------------------------------
Root project
------------------------------------------------------------

compile - Compile classpath for source set 'main'.
\--- org.springframework:spring-core: -> 4.0.3.RELEASE
     \--- commons-logging:commons-logging:1.1.3 -> 1.1.2
```

When you want to provide dependency management for multiple modules with the same group and
version you should use a dependency set. Using a dependency set removes the need to specify
the same group and version multiple times:

```groovy
dependencyManagement {
     dependencies {
          dependencySet(group:'org.slf4j', version: '1.7.7') {
               entry 'slf4j-api'
               entry 'slf4j-simple'
          }
     }
}
```

### Importing a Maven bom

The plugin also allows you to import an existing Maven bom to utilise its dependency management.
For example:

```groovy
dependencyManagement {
     imports {
          mavenBom 'io.spring.platform:platform-bom:1.0.1.RELEASE'
     }
}

dependencies {
     compile 'org.springframework.integration:spring-integration-core'
}
```

This configuration will apply the [versions in the Spring IO Platform bom][2] to the project's
dependencies:

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

It's provided a version of `4.0.2.RELEASE` for the `spring-integration-core` dependency. It has
also set the version of all of the Spring Framework dependencies to `4.0.6.RELEASE`

#### Overriding versions in a bom

When the bom is being processed, Gradle's properties are used as a source during the property
resolution process. If the bom is written to use properties for its versions, this allows you to
override a version.

Building on the example above, the Spring IO Platform bom that is used contains a property
named `spring.version`. This property determines the version of all of the Spring Framework modules
and, by default, its value is `4.0.6.RELEASE`. It can be overridden by configuring the
`spring.version` property via any of the mechanisms that Gradle provides. For example, you may
choose to configure it in your `build.gradle` script:

```groovy
ext['spring.version'] = '4.0.4.RELEASE'
```

Or in `gradle.properties`

```
spring.version=4.0.4.RELEASE
```

Wherever you configure it, the version of any Spring Framework modules will now match the value of
the property:

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

### Dependency management for specific configurations

To target dependency management at a single configuration, you nest the dependency management
within a block named after the configuration. For example, the following will apply dependency
management to the compile configuration:

```groovy
dependencyManagement {
     compile {
          dependencies {
               …
          }
          imports {
               …
          }
     }
}
```

To target dependency management at multiple configurations, you use `configurations` to list the
configurations to which the dependency management should be applied. For example, the following
will apply dependency management to the compile and custom configurations:

```groovy
dependencyManagement {
     configurations(compile, custom) {
          dependencies {
               …
          }
          imports {
               …
          }
     }
}
```

## Accessing the managed versions

The plugin provides an API for accessing the versions provided by the configured dependency
management. Accessing versions from global dependency management:

```groovy
dependencyManagement.getManagedVersion('org.springframework', 'spring-core')
```

Accessing versions from configuration-specific dependency management:

```groovy
dependencyManagement.forConfiguration('compile').getManagedVersion('org.springframework', 'spring-core')
```

[1]: http://repo.spring.io/plugins-snapshot/io/spring/gradle/dependency-management-plugin
[2]: http://docs.spring.io/platform/docs/1.0.1.RELEASE/reference/htmlsingle/#appendix-dependency-versions