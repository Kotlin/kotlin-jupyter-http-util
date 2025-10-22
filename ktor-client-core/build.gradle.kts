plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    alias(libs.plugins.publisher)
}

kotlinJupyter {
    addApiDependency()
}

tasks.processJupyterApiResources {
    libraryProducers = listOf(
        "org.jetbrains.kotlinx.jupyter.ktor.client.core.KtorClientCoreIntegration"
    )
}

dependencies {
    api(libs.ktor.client.core)
    runtimeOnly(libs.ktor.client.cio)
    compileOnly(libs.dataframe)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
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