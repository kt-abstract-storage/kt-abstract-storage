# kt-abstract-storage

`kt-abstract-storage` is a Kotlin/JVM library that provides abstractions and implementations for:

- file and folder storage interfaces
- in-memory and system-backed storage
- stream adapters and wrappers
- ZIP archive-backed virtual folders/files

## Modules

- `:app` - publishable core library (`kt-abstract-storage-core`)
- `buildSrc` - shared Gradle convention plugin

## Build and test

Use the Gradle wrapper from the repository root:

- `./gradlew build`
- `./gradlew test`
- `./gradlew :app:test`
- `./gradlew clean`

## Publishing

The shared convention configures (via `com.vanniktech.maven.publish`):

- reproducible jars
- `sourcesJar` and `javadocJar`
- Maven publications
- optional signing
- Maven Central publication via the Central Portal

Coordinates and POM metadata are controlled through `gradle.properties` and module-level `mavenPublishing { coordinates(...) }` blocks.

For Maven Central + signing credentials, set:

- `mavenCentralUsername`
- `mavenCentralPassword`
- `signingInMemoryKey`
- `signingInMemoryKeyPassword`

### Local publish

- `./gradlew publishToMavenLocal`
- `./gradlew :app:publishToMavenLocal`

### Publish to configured remotes

- `./gradlew publish`
- `./gradlew :app:publish`

If remote credentials/signing keys are missing, only local publication is expected to work.

## Upstream parity tests

The test suite includes Kotlin/JUnit ports derived from `OwlCore.Storage.Tests` (excluding HTTP-specific tests).
