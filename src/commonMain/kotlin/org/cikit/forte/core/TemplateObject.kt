package org.cikit.forte.core

interface TemplateObject {
    fun getVar(name: String): Any?
    fun getFunction(key: Context.Key.Call): Function? = null
}
