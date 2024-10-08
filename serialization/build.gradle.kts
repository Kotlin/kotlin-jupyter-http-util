plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    publisher
}

dependencies {
    compileOnly(libs.kotlinx.serialization.json)
    implementation(libs.kotlin.reflect)
    implementation(project(":json2kt"))

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.serialization.json)
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
    explicitApi()
}
