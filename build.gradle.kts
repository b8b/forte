@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    `maven-publish`
}

group = "org.cikit"
version = "0.8.3-dev"

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
            testTask {
                enabled = false
            }
        }
    }
    /* wip
    wasmWasi {
        nodejs {
            binaries.executable()
            runTask {
                executable = file("work/node-v24.8.0-linux-x64/bin/node").path
                nodeArgs.add("--experimental-wasm-exnref")
                nodeArgs.add("--no-warnings")
            }
        }
    }
    */
    sourceSets {
        commonMain {
            dependencies {
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
                api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
                api("org.jetbrains.kotlinx:kotlinx-io-bytestring:0.8.0")
                api("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.4.0")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-properties:1.9.0")
            }
        }
        jvmMain {}
        jvmTest {}
        jsMain {
            dependencies {
                implementation("dev.erikchristensen.javamath2kmp:javamath2kmp:1.1")
            }
        }
        jsTest {}
        wasmJsMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-browser-wasm-js:0.5.0")
                implementation("com.ionspin.kotlin:bignum:0.3.10")
                implementation("dev.erikchristensen.javamath2kmp:javamath2kmp:1.1")
            }
        }
        wasmJsTest {}
        /* wip
        wasmWasiMain {}
        wasmWasiTest {}
        */
    }
}

/* wip
tasks.withType<org.jetbrains.kotlin.gradle.tasks.Kotlin2JsCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xwasm-use-new-exception-proposal")
    }
}

tasks.register<Exec>("runWasmtime") {
    dependsOn("compileProductionExecutableKotlinWasmWasiOptimize")
    executable = "wasmtime"
    args = listOf(
        "run",
        "-W", "gc=y",
        "-W", "gc-support=y",
        "-W", "exceptions=y",
        "-W", "function-references=y",
        file("build/compileSync/wasmWasi/main/productionExecutable/optimized/forte.wasm").path,
        "{{ 1 + 1 }}"
    )
}
*/

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
