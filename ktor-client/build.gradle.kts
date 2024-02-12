plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    `maven-publish`
}

kotlinJupyter {
    addApiDependency()
    addScannerDependency()
}

dependencies {
    testImplementation(libs.kotlin.test)
    api(libs.ktor.client.core)
    runtimeOnly(libs.ktor.client.apache)
    api(libs.ktor.client.content.negotiation)
    api(libs.ktor.serialization.kotlinx.json)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            this.artifactId = "kotlin-jupyter-ktor-client"
            from(components["kotlin"])
        }
    }
}
