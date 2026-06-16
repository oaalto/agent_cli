plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
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
    }
    implementation("com.agentclientprotocol:acp:0.24.0")
    testImplementation(kotlin("test"))
    detektPlugins("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
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
        dependsOn("ktlintCheck", "detekt")
        finalizedBy("jacocoTestReport")
    }

    named<JacocoReport>("jacocoTestReport") {
        dependsOn("test")
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
        dependsOn("jacocoTestReport")
        violationRules {
            rule {
                element = "BUNDLE"
                limit {
                    counter = "LINE"
                    minimum = "0.10".toBigDecimal()
                }
            }
        }
    }

    register("qualityGate") {
        group = "verification"
        description = "Runs format, compile, lint, static analysis, tests, and coverage verification."
        dependsOn("test", "jacocoTestCoverageVerification")
    }

    named("check") {
        dependsOn("ktlintCheck", "detekt", "jacocoTestCoverageVerification")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        allWarningsAsErrors.set(true)
    }
}

ktlint {
    ignoreFailures.set(false)
}
