plugins {
    kotlin("multiplatform")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvm()
    js(IR) {
        nodejs()
    }
    linuxX64()
    mingwX64()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinxCoroutinesCore)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinxCoroutinesTest)
            implementation(project(":storage-common-tests"))
        }

        jvmTest.dependencies {
            implementation(libs.junitJupiterApi)
            runtimeOnly(libs.junitJupiterEngine)
            runtimeOnly(libs.junitPlatformLauncher)
        }
    }
}

mavenPublishing {
    coordinates(group.toString(), "commonLib")
    publishToMavenCentral(SonatypeHost.S01)
}
