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
    implementation("org.jetbrains.kotlinx:kotlinx-io-core-jvm:0.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

mavenPublishing {
    coordinates(group.toString(), "kt-abstract-storage-kotlinx-io-files", version.toString())
}

