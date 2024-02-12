plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    `maven-publish`
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

publishing {
    publications {
        create<MavenPublication>("maven") {
            this.artifactId = "kotlin-jupyter-json-generation"
            from(components["kotlin"])
        }
    }
}
