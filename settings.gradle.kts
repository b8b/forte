pluginManagement {
    plugins {
        val kotlinVersion = "2.2.20"
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

rootProject.name = "forte"
