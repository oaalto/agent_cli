plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

val defaultPluginVersion = "1.0"
val resolvedPluginVersion = providers.gradleProperty("pluginVersion").orElse(defaultPluginVersion)
val generatedChangeNotes = providers.gradleProperty("pluginChangeNotesFile").map { relativePath ->
    val changeNotesFile = layout.projectDirectory.file(relativePath).asFile
    require(changeNotesFile.isFile) {
        "pluginChangeNotesFile does not point to a readable file: $relativePath"
    }
    changeNotesFile.readText(Charsets.UTF_8).trim()
}

group = "com.oaalto"
version = resolvedPluginVersion.get()

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
    }
}

intellijPlatform {
    pluginConfiguration {
        version = resolvedPluginVersion
        ideaVersion {
            sinceBuild = "253"
        }

        val defaultChangeNotes = """
            Initial version
        """.trimIndent()
        changeNotes = generatedChangeNotes.orElse(defaultChangeNotes)
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
