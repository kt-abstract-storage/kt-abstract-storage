import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.multiplatform") apply false
    id("org.jetbrains.kotlin.android") apply false
    id("com.android.library") apply false
    id("com.vanniktech.maven.publish") apply false
}

allprojects {
    group = providers.gradleProperty("POM_GROUP_ID").getOrElse("io.github.kt-abstract-storage")
    version = providers.gradleProperty("VERSION_NAME").getOrElse("0.1.0-SNAPSHOT")
}

subprojects {
    plugins.withId("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            publishToMavenCentral(SonatypeHost.S01)
        }
    }
}
