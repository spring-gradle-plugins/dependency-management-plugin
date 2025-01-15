# Contributing to Dependency Management Plugin

Dependency Management Plugin is released under the Apache 2.0 license. If you would like
to contribute something, or simply want to work with the code, this document should help
you to get started.

## Code of conduct

This project adheres to the Contributor Covenant [code of conduct][1]. By participating,
you are expected to uphold this code. Please report unacceptable behavior to
spring-code-of-conduct@pivotal.io.

## Include a Signed-off-by Trailer

All commits must include a _Signed-off-by_ trailer at the end of each commit message to
indicate that the contributor agrees to the [Developer Certificate of Origin (DCO)][2].
For additional details, please refer to the ["Hello DCO, Goodbye CLA: Simplifying
Contributions to Spring"][3] blog post.

## Code conventions and housekeeping

None of these is essential for a pull request, but they will all help

- Make sure all new `.groovy` files to have a simple Javadoc class comment with at least an
  `@author` tag identifying you, and preferably at least a paragraph on what the class is
  for.
- Add the ASF license header comment to all new `.groovy` files (copy from existing files
  in the project)
- Add yourself as an `@author` to the `.groovy` files that you modify substantially (more
  than cosmetic changes).
- Add some Javadocs
- Add unit tests that covers and new or modified functionality
- Whenever possible, please rebase your branch against the current main (or other
  target branch in the main project).
- When writing a commit message please follow [these conventions][4]. Also, if you are
  fixing an existing issue please add `Fixes gh-nnn` at the end of the commit message
  (where nnn is the issue number).

## Working with the code

### Building from source

The code is built with Gradle:

```
$ ./gradlew build
```

[1]: CODE_OF_CONDUCT.md
[2]: https://en.wikipedia.org/wiki/Developer_Certificate_of_Origin
[3]: https://spring.io/blog/2025/01/06/hello-dco-goodbye-cla-simplifying-contributions-to-spring
[4]: https://tbaggery.com/2008/04/19/a-note-about-git-commit-messages.html
