plugins {
    java
    application
    id("io.freefair.lombok") version "8.13.1"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "art.galushko"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    // GitLab API client
    implementation("org.gitlab4j:gitlab4j-api:5.8.0")

    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.19.0")

    // CLI argument parsing
    implementation("info.picocli:picocli:4.7.7")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.mockito:mockito-core:5.18.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.18.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.wiremock:wiremock:3.13.0")
}

application {
    mainClass.set("art.galushko.gitlab.mrconflict.cli.ConflictDetectorApplication")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

tasks.shadowJar {
    archiveBaseName.set("gitlab-mr-conflict-detector")
    archiveClassifier.set("")
    mergeServiceFiles()
}

// Disable conflicting distribution tasks since we're using shadowJar
tasks.named("distZip") {
    enabled = false
}

tasks.named("distTar") {
    enabled = false
}

// Disable jar to avoid dependency conflicts
tasks.named("jar") {
    enabled = false
}

// Configure startScripts to work with shadowJar
tasks.named("startScripts") {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "art.galushko.gitlab.mrconflict.cli.ConflictDetectorApplication",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
}

tasks.compileTestJava {
    options.encoding = "UTF-8"
}
