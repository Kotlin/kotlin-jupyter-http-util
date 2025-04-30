plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    alias(libs.plugins.publisher)
}

kotlinJupyter {
    addApiDependency()
    addScannerDependency()
}

dependencies {
    api(libs.ktor.client.core)
    runtimeOnly(libs.ktor.client.cio)
    compileOnly(libs.dataframe)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
    explicitApi()
}

kotlinPublications {
    publication {
        description.set("Library for making HTTP requests using Ktor client")
    }
}