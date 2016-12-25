package com.github.takahirom.logger

import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Plugin

import java.lang.reflect.Method

class LoggerPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def isAndroidApp = project.plugins.withType(AppPlugin)
        def isAndroidLib = project.plugins.withType(LibraryPlugin)
        if (!isAndroidApp && !isAndroidLib) {
            throw new GradleException("'com.android.application' or 'com.android.library' plugin required.")
        }

        if (!isTransformAvailable()) {
            throw new GradleException('LoggerPlugin gradle plugin only supports android gradle plugin 2.0.0 or later')
        }

        project.android.registerTransform(new LoggerTransformer(project))
    }

    private static boolean isTransformAvailable() {
        try {
            Class transform = Class.forName('com.android.build.api.transform.Transform')
            Method transformMethod = transform.getMethod("transform", [TransformInvocation.class] as Class[])
            return transformMethod != null
        } catch (Exception ignored) {
            return false
        }
    }
}
