pluginManagement {
    repositories {
        maven("https://nexus.modlabs.cc/repository/maven-mirrors/")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.7.5"
}

stonecutter {
    create(rootProject) {
        versions(
            "1.21.4", "1.21.6", "1.21.7", "1.21.8",
        )
        vcsVersion = "1.21.8"
    }
}