plugins {
    kotlin("multiplatform") version "1.8.20"
    `maven-publish`
}

group = "org.cikit"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        jvmToolchain(8)
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js(IR) {
        nodejs {
            binaries.executable()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
                api("com.squareup.okio:okio:3.3.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("org.mozilla:rhino:1.7.14")
            }
        }
        val jvmTest by getting
        val jsMain by getting
        val jsTest by getting
    }
}

publishing {
    publications.configureEach {
        this as MavenPublication
        pom {
            name.set("forte")
            description.set("Twig like template engine")
            url.set("https://github.com/b8b/forte")
            licenses {
                license {
                    name.set("The Apache License, Version 2.0")
                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("b8b@cikit.org")
                    name.set("b8b@cikit.org")
                    email.set("b8b@cikit.org")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/b8b/forte.git")
                developerConnection.set("scm:git:ssh://github.com/b8b/forte.git")
                url.set("https://github.com/b8b/forte.git")
            }
        }
    }
}
