allprojects {
    group = providers.gradleProperty("POM_GROUP_ID").getOrElse("io.github.kt-abstract-storage")
    version = providers.gradleProperty("VERSION_NAME").getOrElse("0.1.0-SNAPSHOT")
}

