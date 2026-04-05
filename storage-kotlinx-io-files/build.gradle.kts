plugins {
    kotlin("jvm")
    `java-library`
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":commonLib"))
    implementation(project(":storage-jvm-core"))
    implementation(libs.kotlinxIoCoreJvm)
    implementation(libs.kotlinxCoroutinesCore)

    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(project(":storage-common-tests"))
    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.junitJupiterEngine)
    testRuntimeOnly(libs.junitPlatformLauncher)
}

mavenPublishing {
    publishToMavenCentral()
    coordinates(group.toString(), "kt-abstract-storage-kotlinx-io-files", version.toString())
}
