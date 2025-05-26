@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    `maven-publish`
}

group = "org.cikit"
version = "0.5.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
    jvm {
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }
    js {
        nodejs {
            binaries.executable()
        }
    }
    wasmJs {
        nodejs {
            binaries.executable()
        }
        browser {
            binaries.executable()
        }
    }
    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
                api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.7.0")
                api("org.jetbrains.kotlinx:kotlinx-io-core:0.7.0")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
            }
        }
        jvmMain {}
        jvmTest {}
        jsMain {}
        jsTest {}
        wasmJsMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-browser-wasm-js:0.3")
            }
        }
        wasmJsTest {}
    }
}

publishing {
    publications {
        withType(MavenPublication::class.java).configureEach {
            pom {
                name = "forte"
                description = "Twig like template engine"
                url = "https://github.com/b8b/forte"
                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "b8b@cikit.org"
                        name = "b8b@cikit.org"
                        email = "b8b@cikit.org"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/b8b/forte.git"
                    developerConnection = "scm:git:ssh://github.com/b8b/forte.git"
                    url = "https://github.com/b8b/forte.git"
                }
            }
        }
    }
}
