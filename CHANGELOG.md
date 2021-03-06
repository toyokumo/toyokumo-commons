# Change Log

## [Unreleased]

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
