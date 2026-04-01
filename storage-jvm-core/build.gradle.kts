plugins {
    id("buildsrc.convention.kotlin-jvm")
}

dependencies {
    implementation(project(":commonLib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}

mavenPublishing {
    coordinates(group.toString(), "kt-abstract-storage-jvm-core", version.toString())
}


