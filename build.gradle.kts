plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.10"
    id("org.jetbrains.intellij") version "1.13.3"
}

group = "com.omniviewer"
version = "0.1.3"

repositories {
    mavenCentral()
}

dependencies {
    // Java Sound API is included in JDK, no additional dependencies needed
    // MP3 support libraries
    implementation("javazoom:jlayer:1.0.1")
    implementation("com.googlecode.soundlibs:mp3spi:1.9.5.4")
    implementation("com.googlecode.soundlibs:tritonus-share:0.3.7-2")
}

intellij {
    version.set("2023.2")
    // Support multiple IDEs - use IC as base for compatibility
    type.set("IC") // IntelliJ IDEA Community Edition (base platform)
    
    // Use only core platform - compatibility handled via plugin.xml
    // plugins.set(listOf("com.intellij.java"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
        
        // Plugin compatibility for multiple IDEs
        pluginDescription.set("""
            <h2>Omni Viewer</h2>
            <p>A powerful plugin for viewing and editing various file formats including audio, video, images, CSV, and JSONL files.</p>
            
            <h3>Features:</h3>
            <ul>
                <li><strong>Audio Viewer:</strong> Play, pause, and control audio files directly in your IDE</li>
                <li><strong>Video Viewer:</strong> Watch video files with playback controls</li>
                <li><strong>Image Viewer:</strong> View and edit images with zoom and pan capabilities</li>
                <li><strong>CSV Viewer:</strong> View and edit CSV files in a table format</li>
                <li><strong>JSONL Viewer:</strong> View and edit JSON Lines files</li>
            </ul>
            
            <p>Compatible with IntelliJ IDEA, GoLand, PhpStorm, WebStorm, and other JetBrains IDEs!</p>
        """.trimIndent())
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
