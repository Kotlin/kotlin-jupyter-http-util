plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "notebooks-ktor-client"
include("json-generation")
include("ktor-client")


dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            val kotlinVersion = "1.9.22"
            plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").version(kotlinVersion)
            library("kotlin-test", "org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
            library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

            val ktorVersion = "2.3.7"
            library("ktor-client-core", "io.ktor:ktor-client-core-jvm:$ktorVersion")
            library("ktor-client-apache", "io.ktor:ktor-client-apache-jvm:$ktorVersion")
            library("ktor-client-content-negotiation", "io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
            library("ktor-serialization-kotlinx-json", "io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")

            plugin("kotlin-jupyter-api", "org.jetbrains.kotlin.jupyter.api").version("0.12.0-110")

            plugin("ksp", "com.google.devtools.ksp").version("1.9.22-1.0.17")
        }
    }
}

