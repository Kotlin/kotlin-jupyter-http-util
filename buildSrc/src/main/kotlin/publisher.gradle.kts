import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension

plugins {
    `maven-publish`
    id("signing")
}

val spaceUsername: String = System.getenv("SPACE_USERNAME") ?: ""
val spaceToken: String = System.getenv("SPACE_TOKEN") ?: ""

// Maven Central publishing properties
val sonatypeUsername: String = System.getenv("SONATYPE_USER") ?: ""
val sonatypePassword: String = System.getenv("SONATYPE_PASSWORD") ?: ""
val signingKey: String? = System.getenv("SIGN_KEY_ID")
val signingPrivateKey: String? = System.getenv("SIGN_KEY_PRIVATE")
val signingKeyPassphrase: String? = System.getenv("SIGN_KEY_PASSPHRASE")

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks["kotlinSourcesJar"]) {
                classifier = "sources"
            }

            artifactId = "kotlin-jupyter-${project.name}"
            from(components["kotlin"])

            // POM information required by Maven Central
            pom {
                name.set("HTTP Utilities for Kotlin Jupyter kernel")
                description.set("HTTP Utilities for Kotlin Jupyter: Ktor client and serialization")
                url.set("https://github.com/Kotlin/kotlin-jupyter-http-util")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("kotlin-jupyter-team")
                        name.set("Kotlin Jupyter Team")
                        organization.set("JetBrains")
                        organizationUrl.set("https://www.jetbrains.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/Kotlin/kotlin-jupyter-http-util.git")
                    developerConnection.set("scm:git:ssh://github.com/Kotlin/kotlin-jupyter-http-util.git")
                    url.set("https://github.com/Kotlin/kotlin-jupyter-http-util")
                }
            }
        }
    }

    repositories {
        maven("https://packages.jetbrains.team/maven/p/kds/kotlin-ds-maven") {
            name = "jbTeam"
            credentials {
                username = spaceUsername
                password = spaceToken
            }
        }

        // Maven Central repository via Sonatype OSSRH
        maven {
            name = "sonatype"
            url = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = sonatypeUsername
                password = sonatypePassword
            }
        }
    }
}

if (signingKey != null) {
    extensions.configure<SigningExtension>("signing") {
        useInMemoryPgpKeys(signingKey, signingPrivateKey, signingKeyPassphrase)
        sign(extensions.getByType<PublishingExtension>().publications["maven"])
    }
}
