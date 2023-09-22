plugins {
    kotlin("jvm") version "1.9.0"
    id("io.papermc.paperweight.userdev") version "1.5.6" // the latest version can be found on the Gradle Plugin Portal
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.ben-manes.versions") version "0.48.0"
}

group = "de.michaelzinn.playerservices"
version = "1.0.0-alpha"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    paperweight.paperDevBundle("1.20.1-R0.1-SNAPSHOT")
    testImplementation(kotlin("test"))
}

tasks.build {
    dependsOn("shadowJar")
}
tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
