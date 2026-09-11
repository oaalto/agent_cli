plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.4.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8"
    jacoco
}

val defaultPluginVersion = "3.0.0-SNAPSHOT"
val resolvedPluginVersion = providers.gradleProperty("pluginVersion").orNull ?: defaultPluginVersion
val defaultChangeNotes =
    """
    3.0.0 development cycle started.
    """.trimIndent()
val resolvedChangeNotes =
    providers.gradleProperty("pluginChangeNotesFile").orNull?.let { relativePath ->
        val changeNotesFile = layout.projectDirectory.file(relativePath).asFile
        require(changeNotesFile.isFile) {
            "pluginChangeNotesFile does not point to a readable file: $relativePath"
        }
        changeNotesFile.readText(Charsets.UTF_8).trim()
    } ?: defaultChangeNotes

group = "com.oaalto"
version = resolvedPluginVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    intellijPlatform {
        intellijIdea("2025.3.2")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        bundledPlugin("org.jetbrains.plugins.terminal")
        bundledPlugin("Git4Idea")
        bundledPlugin("com.intellij.mcpServer")
    }
    implementation("com.agentclientprotocol:acp:0.24.0")
    testImplementation(kotlin("test"))
}

intellijPlatform {
    pluginConfiguration {
        version = resolvedPluginVersion
        ideaVersion {
            sinceBuild = "253"
        }
        changeNotes = resolvedChangeNotes
    }
    pluginVerification {
        ides {
            recommended()
        }
    }
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/detekt.yml"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "21"
        reports {
            html.required.set(true)
            xml.required.set(true)
        }
    }

    named("compileKotlin") {
        dependsOn("ktlintFormat")
    }
    named("ktlintCheck") {
        dependsOn("compileKotlin")
    }
    named("test") {
        dependsOn("ktlintCheck", "detekt", "detektMain", "detektTest")
        finalizedBy("jacocoTestReport")
    }

    named<JacocoReport>("jacocoTestReport") {
        dependsOn("test")
        classDirectories.setFrom(
            layout.buildDirectory.dir("instrumented/instrumentCode/classes"),
            layout.buildDirectory.dir("classes/kotlin/main"),
        )
        sourceDirectories.setFrom(files("src/main/kotlin"))
        executionData.setFrom(layout.buildDirectory.file("jacoco/test.exec"))
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    register("qualityGate") {
        group = "verification"
        description =
            "Runs format, compile, lint, static analysis (detekt with type resolution), tests, and coverage report."
        dependsOn("test", "jacocoTestReport", "detektMain", "detektTest")
    }

    named("check") {
        dependsOn("ktlintCheck", "detekt", "detektMain", "detektTest")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        allWarningsAsErrors.set(true)
        freeCompilerArgs.addAll(
            "-opt-in=com.agentclientprotocol.annotations.UnstableApi",
        )
    }
}

ktlint {
    ignoreFailures.set(false)
}
