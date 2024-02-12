plugins {
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifact(tasks["kotlinSourcesJar"]) {
                classifier = "sources"
            }

            this.artifactId = "kotlin-jupyter-${project.name}"
            from(components["kotlin"])
        }
    }
}