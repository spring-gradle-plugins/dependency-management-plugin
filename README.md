# Dependency Management Plugin

[![Chat on Gitter][1]][2]

A Gradle plugin that provides Maven-like dependency management and exclusions. The
plugin provides a DSL to configure dependency management directly and by importing
existing Maven boms. Based on the configured dependency management, the plugin will
control the versions of your project's direct and transitive dependencies and will honour
any exclusions declared in the poms of your project's dependencies and any imported boms.

To learn more about using the Dependency Management Plugin, please refer to its
[reference documentation][3].

## Contributing

Contributors to this project agree to uphold its [code of conduct][4].
[Pull requests][5] are welcome. Please see the [contributor guidelines][6] for details.

## Licence

Dependency Management Plugin is open source software released under the [Apache 2.0
license][7].

[1]: https://badges.gitter.im/Join%20Chat.svg
[2]: https://gitter.im/spring-gradle-plugins/dependency-management-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge
[3]: https://docs.spring.io/dependency-management-plugin/docs/current-SNAPSHOT/reference/html5/
[4]: CODE_OF_CONDUCT.md
[5]: https://help.github.com/articles/using-pull-requests/
[6]: CONTRIBUTING.md
[7]: http://www.apache.org/licenses/LICENSE-2.0.html
