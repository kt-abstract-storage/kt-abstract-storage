# kt-abstract-storage

`kt-abstract-storage` is a Kotlin/JVM library that provides abstractions and implementations for:

- file and folder storage interfaces
- in-memory and system-backed storage
- stream adapters and wrappers
- ZIP archive-backed virtual folders/files

## Modules

- `:commonLib` - multiplatform core abstractions (`kt-abstract-storage-core`)
- `:storage-common-tests` - shared test contracts (`kt-abstract-storage-common-tests`)
- `:storage-jvm-core` - Android-safe JVM implementations (`kt-abstract-storage-jvm-core`)
- `:storage-jvm-nio` - desktop JVM `java.nio.file` implementations (`kt-abstract-storage-jvm-nio`)
- `:storage-android` - Android integration scaffold (`kt-abstract-storage-android`)
- `buildSrc` - shared Gradle convention plugin

## Build and test

Use the Gradle wrapper from the repository root:

- `./gradlew build`
- `./gradlew test`
- `./gradlew :commonLib:jvmTest`
- `./gradlew clean`

## Publishing

The shared convention configures (via `com.vanniktech.maven.publish`):

- reproducible jars
- `sourcesJar` and `javadocJar`
- Maven publications
- optional signing
- Maven Central publication via the Central Portal

Coordinates and POM metadata are controlled through `gradle.properties` and module-level `mavenPublishing { coordinates(...) }` blocks.

Publishable artifacts are produced from `:commonLib`, `:storage-common-tests`, `:storage-jvm-core`, `:storage-jvm-nio`, and `:storage-android`.

For Maven Central + signing credentials, set:

- `mavenCentralUsername`
- `mavenCentralPassword`
- `signingInMemoryKey`
- `signingInMemoryKeyPassword`

### GitHub Actions publish

Publishing automation is defined in `.github/workflows/publish.yml`.

- Triggers: manual dispatch (`workflow_dispatch`) and git tags matching `v*` (for example `v0.2.0`).
- Publish command: `./gradlew publish --stacktrace`.
- The workflow maps GitHub secrets to Gradle properties through `ORG_GRADLE_PROJECT_*` environment variables.

Required repository secrets:

- `MAVEN_CENTRAL_USERNAME` -> `mavenCentralUsername`
- `MAVEN_CENTRAL_PASSWORD` -> `mavenCentralPassword`
- `SIGNING_IN_MEMORY_KEY` -> `signingInMemoryKey`
- `SIGNING_IN_MEMORY_KEY_PASSWORD` -> `signingInMemoryKeyPassword`

Notes:

- `SNAPSHOT` versions publish to Sonatype snapshot repositories.
- Release versions (non-`SNAPSHOT`) publish to Maven Central.
- Signing is enabled for non-`SNAPSHOT` versions when signing credentials are present.

### Local publish

- `./gradlew publishToMavenLocal`
- `./gradlew :commonLib:publishToMavenLocal`
- `./gradlew :storage-common-tests:publishToMavenLocal`
- `./gradlew :storage-jvm-core:publishToMavenLocal`
- `./gradlew :storage-jvm-nio:publishToMavenLocal`
- `./gradlew :storage-android:publishToMavenLocal`

### Publish to configured remotes

- `./gradlew publish`
- `./gradlew :commonLib:publish`
- `./gradlew :storage-common-tests:publish`
- `./gradlew :storage-jvm-core:publish`
- `./gradlew :storage-jvm-nio:publish`
- `./gradlew :storage-android:publish`

If remote credentials/signing keys are missing, only local publication is expected to work.

