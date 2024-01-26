plugins {
    id("com.google.devtools.ksp") version "1.9.21-1.0.15"
    kotlin("jvm") version "1.9.21"
    kotlin("jupyter.api") version "0.12.0-110"
    `maven-publish`
}

group = "org.jetbrains"
version = "0.1.0"

repositories {
    mavenCentral()
}

kotlinJupyter {
    addApiDependency()
    addScannerDependency()
}

dependencies {
    val ktorVersion = "2.3.7"

    testImplementation(kotlin("test"))
    api("io.ktor:ktor-client-core-jvm:$ktorVersion")
    runtimeOnly("io.ktor:ktor-client-apache-jvm:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation-jvm:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
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
            from(components["kotlin"])
        }
    }
}
