# Change Log

## [Unreleased]
### Added
- Add `with-graceful-shutdown` macro.

## 0.3.199
### Breaking
- Add `enable-introspection` option to Lacinia component. If you want to enable introspection, you need to set `enable-introspection` to `true` explicitly.

## 0.3.195
### Changed
- Bump whole dependencies.

## 0.3.182
### Changed
- Updated to use `org.eclipse.angus/angus-mail` instead of `com.sun.mail/jakarta.mail`.
  - Jakarta Mail implementation had been moved to Eclipse Angus.

## 0.3.174
### Changed
- Bumped lacinia to 1.2.

## 0.3.171
### Changed
- Enabled `toyokumo.commons.experimental.firebase.admin-sdk/FirebaseAdmin` to specify `FirebaseOptions$Builder` via the `options-builder` option.

## 0.3.167
### Fixed
- Fixed `toyokumo.commons.ring.response/attachment` to work correctly with Safari.

## 0.3.164
### Changed
- Changed to use build.edn to build and deploy this library.
### Breaking
- Make SendGrid API base URL customizable and remove them from vars.

## 0.3.156
### Changed
- Exclude `slf4j-api` from dependencies.

### Fixed
- Fix `toyokumo.commons.ring.response/attachment` to handle filenames containing spaces correctly.

## 0.3.151
### Breaking
- Remove deprecated codes for PostgreSQL. Use `toyokumo.commons.db.postgresql` and `toyokumo.commons.db.extension.postgresql`

### Fixed
- Fix cljdoc to be able to generate documents correctly.

### Added
- Experimental support for GraphQL.

## 0.3.139
### Added
- Improve and add lint settings
- Add Intellij settings
- Add PostgreSQL specific namespaces

## 0.3.121
### Breaking
- Use deps.edn instead of Leiningen.

### Changed
- Use GitHub actions instead of CircleCI.

## 0.3.0
### Breaking
- JDK 8 support was dropped because updated to Jetty 10.

## 0.2.3
### Changed and breaking
- Bump versions #7, which includes [this breaking change](https://github.com/ptaoussanis/carmine/blob/master/CHANGELOG.md#v300--2020-sep-22).

## 0.2.2
### Added
- Add email quote and unquote util
- Add email protocol and SendGrid implementation

### Fixed
- Fix `qualified-name` to work on cljs

### Changed
- Make transit enable to use from cljs
- Add response types
- Upgrade Jetty9
- Upgrade jakarta mail

## 0.2.1
### Added
- Add `qualified-name`

## 0.2.0
### Added
- Add components such as Jetty9Server, HikariCP and Carmine and functions to use them
- Add ring middlewares such as wrap-health-check and wrap-trailing-slash

### Changed
- Use jsonista for encoding and decoding JSON

## 0.1.5
### Fixed
- Fix `toyokumo.commons.ring.response/attachment` to work on IE

## 0.1.4
### Added
- Add `toyokumo.commons.io/excluding-bom-reader`

## 0.1.3
### Added
- Add session utilities to `toyokumo.commons.ring.response`

## 0.1.2
### Added
- Add `charset` option to `url-encode`, `url-decode` in `toyokumo.commons.url` and `html`, `json` and `csv` in `toyokumo.commons.ring.response`

### Breaking
- Divide `toyokumo.commons.ring.response/csv` into `attachment` and `csv`

## 0.1.1
### Fixed
- Remove schema from a cljc file

## 0.1.0
Initial release.
