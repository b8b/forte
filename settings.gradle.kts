pluginManagement {
    plugins {
        val kotlinVersion = "2.3.0"
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

rootProject.name = "forte"
