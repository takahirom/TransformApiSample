@file:JvmName("LoggerPlugin")

package com.github.takahirom.logger

import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

class LoggerPlugin : Plugin<Project> {
    @Override
    override fun apply(project: Project) {
        val isAndroidApp = project.plugins.withType(AppPlugin::class.java)
        val isAndroidLib = project.plugins.withType(LibraryPlugin::class.java)
        if (isAndroidApp == null && isAndroidLib == null) {
            throw GradleException("com.android.application' or 'com.android.library' plugin required.")
        }

        if (!isTransformAvailable()) {
            throw GradleException("LoggerPlugin gradle plugin only supports android gradle plugin 2.0.0 or later")
        }

        val transform = LoggerTransformer(project)
        project.baseExtention()?.registerTransform(transform)
    }

    private fun isTransformAvailable(): Boolean {
        try {
            val transform = Class.forName("com.android.build.api.transform.Transform")
            val transformMethod = transform.getMethod("transform", TransformInvocation::class.java)
            return transformMethod != null
        } catch (ignored: Exception) {
            return false
        }
    }
}
