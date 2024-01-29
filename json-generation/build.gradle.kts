plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    `maven-publish`
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation(libs.kotlin.reflect)
    implementation("com.sealwu.jsontokotlin:library:3.7.4")

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
    jvmToolchain(17)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            this.artifactId = "kotlin-jupyter-json-generation"
            from(components["kotlin"])
        }
    }
}
