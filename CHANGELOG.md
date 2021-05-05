# Changelog

This project follows semantic versioning.

Possible log types:

- `[added]` for new features.
- `[changed]` for changes in existing functionality.
- `[deprecated]` for once-stable features removed in upcoming releases.
- `[removed]` for deprecated features removed in this release.
- `[fixed]` for any bug fixes.
- `[security]` to invite users to upgrade in case of vulnerabilities.


### v1.0.1 (2021-05-05)

- [changed] Upgrade to Gradle 6
- [changed] Drop Java 7
- [changed] Move from Bintray / JCenter to Maven Central

### v1.0.0 (2016-12-17)

- No change compared to v0.2.0. But the relase should be stable now.

### v0.2.0 (2016-09-27)

- [changed] The chosen chunk size now includes the header size

### v0.1.5 (2016-09-06)

- [fixed] Validate bounds of message id in `Chunker`
- [fixed] Fix handling of repeated chunks with same message id and serial

### v0.1.4 (2016-08-18)

- Publish to jcenter

### v0.1.3 (2016-08-17)

- Initial working release
