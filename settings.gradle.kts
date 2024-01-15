pluginManagement {
    plugins {
        val kotlinVersion = "1.9.21"
        kotlin("multiplatform") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
    }
}

rootProject.name = "forte"
