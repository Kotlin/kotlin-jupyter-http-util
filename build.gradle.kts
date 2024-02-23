val buildNumber: String? = properties["build_counter"]?.toString()
val thisVersion = version.toString() + if (buildNumber == null) "" else "-dev-$buildNumber"

allprojects {
    group = "org.jetbrains.kotlinx"
    version = thisVersion

    repositories {
        mavenCentral()
    }
}
