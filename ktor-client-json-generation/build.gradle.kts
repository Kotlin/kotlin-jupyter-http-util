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
    api(project(":ktor-client"))
    api(project(":json-generation"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
    explicitApi()
}

