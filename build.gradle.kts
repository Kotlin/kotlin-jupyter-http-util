import org.jetbrains.kotlinx.publisher.apache2
import org.jetbrains.kotlinx.publisher.githubRepo

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.publisher)
}

val buildNumber: String? = properties["build_counter"]?.toString()
val thisVersion = version.toString() + if (buildNumber == null) "" else "-dev-$buildNumber"

allprojects {
    group = "org.jetbrains.kotlinx"
    version = thisVersion

    repositories {
        mavenCentral()
    }
}

kotlinPublications {
    fairDokkaJars = false
    defaultArtifactIdPrefix = "kotlin-jupyter-"

    // Space publishing properties
    val spaceUsername: String = System.getenv("SPACE_USERNAME") ?: ""
    val spaceToken: String = System.getenv("SPACE_TOKEN") ?: ""

    // Maven Central publishing properties
    val sonatypeUsername: String = System.getenv("SONATYPE_USER") ?: ""
    val sonatypePassword: String = System.getenv("SONATYPE_PASSWORD") ?: ""
    val signingKey: String? = System.getenv("SIGN_KEY_ID")
    val signingPrivateKey: String? = System.getenv("SIGN_KEY_PRIVATE")
    val signingKeyPassphrase: String? = System.getenv("SIGN_KEY_PASSPHRASE")

    sonatypeSettings(
        sonatypeUsername,
        sonatypePassword,
        "kotlin-jupyter-http-util project, v. ${project.version}",
    )

    signingCredentials(
        signingKey,
        signingPrivateKey,
        signingKeyPassphrase,
    )

    pom {
        githubRepo("Kotlin", "kotlin-jupyter-http-util")
        inceptionYear = "2024"
        licenses {
            apache2()
        }
        developers {
            developer {
                id.set("kotlin-jupyter-team")
                name.set("Kotlin Jupyter Team")
                organization.set("JetBrains")
                organizationUrl.set("https://www.jetbrains.com")
            }
        }
    }

    remoteRepositories {
        maven("https://packages.jetbrains.team/maven/p/kds/kotlin-ds-maven") {
            name = "jbTeam"
            credentials {
                username = spaceUsername
                password = spaceToken
            }
        }
    }

    localRepositories {
        maven {
            name = "local"
            url = project.file(layout.buildDirectory.dir("maven")).toURI()
        }
    }
}
