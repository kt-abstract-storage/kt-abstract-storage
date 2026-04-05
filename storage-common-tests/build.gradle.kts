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
            api(project(":commonLib"))
            api(kotlin("test"))
            api(kotlin("test-annotations-common"))
            api(libs.kotlinxCoroutinesTest)
        }

        jvmMain.dependencies {
            api(libs.junitJupiterApi)
        }

        jvmTest.dependencies {
            // JUnit Platform dependencies for running tests
            runtimeOnly(libs.junitJupiterApi)
            runtimeOnly(libs.junitJupiterEngine)
            runtimeOnly(libs.junitPlatformLauncher)
        }
    }
}

mavenPublishing {
    coordinates(group.toString(), "kt-abstract-storage-common-tests")
    publishToMavenCentral(SonatypeHost.S01)
}

