plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publisher)
}

dependencies {
    compileOnly(libs.kotlinx.serialization.json)
    implementation(libs.kotlinpoet)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.serialization.json)
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
