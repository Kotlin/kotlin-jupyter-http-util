plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.jupyter.api)
    alias(libs.plugins.publisher)
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
}

tasks.processJupyterApiResources {
    libraryProducers = listOf(
        "org.jetbrains.kotlinx.jupyter.serialization.SerializationIntegration"
    )
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
        description.set("Library for generating Kotlin code from JSON schemas")
    }
}
