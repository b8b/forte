pluginManagement {
    plugins {
        val kotlinVersion = "2.0.21"
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

rootProject.name = "forte"
