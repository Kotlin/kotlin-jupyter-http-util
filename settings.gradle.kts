plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "notebooks-ktor-client"
include("json2kt")
include("serialization")
include("ktor-client-core")
include("ktor-client")
