plugins {
    `maven-publish`
}

val spaceUsername: String = System.getenv("SPACE_USERNAME") ?: ""
val spaceToken: String = System.getenv("SPACE_TOKEN") ?: ""

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks["kotlinSourcesJar"]) {
                classifier = "sources"
            }

            artifactId = "kotlin-jupyter-${project.name}"
            from(components["kotlin"])
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
    }
}
