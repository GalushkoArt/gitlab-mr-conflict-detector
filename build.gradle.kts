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
    implementation("org.gitlab4j:gitlab4j-api:5.5.0")

    // Git operations
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.8.0.202311291450-r")
    implementation("org.eclipse.jgit:org.eclipse.jgit.ssh.jsch:6.8.0.202311291450-r")

    // HTTP client
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // JSON processing
    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.16.1")

    // CLI argument parsing
    implementation("info.picocli:picocli:4.7.5")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.14")

    // Utilities
    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("commons-io:commons-io:2.15.1")

    // Caching
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // File pattern matching
    implementation("com.github.jknack:handlebars:4.3.1")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.1")
    testImplementation("org.mockito:mockito-core:5.8.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.8.0")
    testImplementation("org.assertj:assertj-core:3.25.1")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.1")
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
