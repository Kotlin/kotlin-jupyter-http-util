plugins {
    alias(libs.plugins.kotlin.jvm)
    publisher
}

dependencies {
    compileOnly(libs.kotlinx.serialization.json)
    implementation(libs.kotlinpoet)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.serialization.json)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(libs.versions.jvm.toolchain.get().toInt())
    explicitApi()
}
