package com.github.takahirom.logger

import com.android.build.gradle.*
import org.gradle.api.Project

fun Project.baseExtention():BaseExtension? = when {
    plugins.hasPlugin(LibraryPlugin::class.java) ->
    extensions.findByType(LibraryExtension::class.java)
    plugins.hasPlugin(TestPlugin::class.java) ->
    extensions.findByType(TestExtension::class.java)
    plugins.hasPlugin(AppPlugin::class.java) ->
    extensions.findByType(AppExtension::class.java)
    else -> {
        null
    }
}