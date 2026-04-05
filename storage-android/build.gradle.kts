import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "io.github.ktabstractstorage.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    api(project(":commonLib"))
    implementation(project(":storage-jvm-core"))
    implementation(libs.annotation)
    implementation(libs.core)
    implementation(libs.kotlinxCoroutinesAndroid)
}

mavenPublishing {
    publishToMavenCentral()
    coordinates(group.toString(), "kt-abstract-storage-android", version.toString())
}
