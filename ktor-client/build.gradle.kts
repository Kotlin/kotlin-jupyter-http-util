plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    publisher
}

kotlinJupyter {
    addApiDependency()
    addScannerDependency()
}

dependencies {
    api(project(":ktor-client-core"))
    api(project(":serialization"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.serialization.json)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.dataframe) {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-json")
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
    explicitApi()
}
