// The code in this file is a convention plugin - a Gradle mechanism for sharing reusable build logic.
// `buildSrc` is a Gradle-recognized directory and every plugin there will be easily available in the rest of the build.
package buildsrc.convention

import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin in JVM projects.
    kotlin("jvm")
    `java-library`
    id("com.vanniktech.maven.publish")
}

kotlin {
    // Use a specific Java version to make it easier to work in different environments.
    jvmToolchain(17)
}

java { withSourcesJar() }

tasks.withType<Jar>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

tasks.withType<Test>().configureEach {
    // Configure all test Gradle tasks to use JUnitPlatform.
    useJUnitPlatform()

    // Log information about all test results, not only the failed ones.
    testLogging {
        events(
            TestLogEvent.FAILED,
            TestLogEvent.PASSED,
            TestLogEvent.SKIPPED
        )
    }
}

mavenPublishing {
    val isSnapshot = version.toString().endsWith("SNAPSHOT", ignoreCase = true)

    // Configure Sonatype publishing for both release and snapshot versions.
    publishToMavenCentral()

    val signingKey = providers.gradleProperty("signingInMemoryKey").orNull
    val signingPassword = providers.gradleProperty("signingInMemoryKeyPassword").orNull

    if (!isSnapshot && !signingKey.isNullOrBlank() && !signingPassword.isNullOrBlank()) {
        signAllPublications()
    }

    pom {
        name.set(providers.gradleProperty("POM_NAME").orElse(project.name))
        description.set(providers.gradleProperty("POM_DESCRIPTION").orElse(project.description ?: project.name))
        url.set(providers.gradleProperty("POM_URL").orElse("https://github.com/kt-abstract-storage/kt-abstract-storage"))

        licenses {
            license {
                name.set(providers.gradleProperty("POM_LICENSE_NAME").orElse("MIT License"))
                url.set(providers.gradleProperty("POM_LICENSE_URL").orElse("https://opensource.org/licenses/MIT"))
            }
        }

        developers {
            developer {
                id.set(providers.gradleProperty("POM_DEVELOPER_ID").orElse("itsWindows11"))
                name.set(providers.gradleProperty("POM_DEVELOPER_NAME").orElse("kt-abstract-storage contributors"))
                email.set(providers.gradleProperty("POM_DEVELOPER_EMAIL").orElse("opensource@example.com"))
            }
        }

        scm {
            url.set(providers.gradleProperty("POM_SCM_URL").orElse("https://github.com/kt-abstract-storage/kt-abstract-storage"))
            connection.set(providers.gradleProperty("POM_SCM_CONNECTION").orElse("scm:git:https://github.com/kt-abstract-storage/kt-abstract-storage.git"))
            developerConnection.set(providers.gradleProperty("POM_SCM_DEV_CONNECTION").orElse("scm:git:ssh://git@github.com/kt-abstract-storage/kt-abstract-storage.git"))
        }
    }
}

extensions.configure<PublishingExtension> {
    repositories {
        mavenLocal()
    }
}
