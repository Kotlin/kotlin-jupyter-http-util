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
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
    explicitApi()
}
