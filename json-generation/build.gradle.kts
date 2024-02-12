plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    publisher
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.reflect)
    implementation(libs.jsontokotlin)

    testImplementation(libs.kotlin.test)
}

kotlinJupyter {
    addApiDependency()
    addScannerDependency()
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
}
