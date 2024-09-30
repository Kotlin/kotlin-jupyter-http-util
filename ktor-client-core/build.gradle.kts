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
    testImplementation(libs.kotlin.test)
    testImplementation(libs.dataframe)
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
