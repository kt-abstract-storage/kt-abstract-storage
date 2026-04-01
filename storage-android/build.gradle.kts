plugins {
    id("com.android.library")
    id("com.vanniktech.maven.publish")
}

android {
    namespace = "io.github.ktabstractstorage.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    coordinates(group.toString(), "kt-abstract-storage-android", version.toString())
}





