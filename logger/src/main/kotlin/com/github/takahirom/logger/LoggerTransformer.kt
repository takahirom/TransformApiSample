package com.github.takahirom.logger

import com.android.SdkConstants
import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent.*
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInput
import com.android.build.api.transform.TransformInvocation
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import javassist.ClassPool
import javassist.LoaderClassPath
import javassist.Modifier
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import java.io.File
import java.util.*

/**
 * from {@url https://github.com/kobito-kaba/TransformApiSample/blob/master/Sample1/logger/src/main/groovy/jp/co/yahoo/sample/logger/LoggerTransformer.groovy}
 */
class LoggerTransformer(val project: Project) : Transform() {


    override fun getName(): String {
        return "LoggerTransformer"
    }

    override fun getInputTypes(): Set<ContentType> {
        return ImmutableSet.of(DefaultContentType.CLASSES)
    }

    override fun getScopes(): MutableSet<Scope> {
        return Sets.newHashSet(Scope.PROJECT)
    }

    override fun getReferencedScopes(): MutableSet<Scope> {
        return Sets.newHashSet(Scope.EXTERNAL_LIBRARIES, Scope.PROJECT_LOCAL_DEPS,
                Scope.SUB_PROJECTS, Scope.SUB_PROJECTS_LOCAL_DEPS, Scope.TESTED_CODE)
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)

        val outputProvider = transformInvocation.outputProvider

        val outputDir = outputProvider.getContentLocation("logger", inputTypes, scopes, Format.DIRECTORY)


        val inputs = transformInvocation.getInputs()
        val classNames = getClassNames(inputs)

        val allList = arrayListOf<TransformInput>()

        allList += inputs
        allList += transformInvocation.getReferencedInputs()
        val classPool = getClassPool(allList)

        modify(classNames, classPool)

        classNames.forEach {
            val ctClass = classPool.getCtClass(it)
            ctClass.writeFile(outputDir.canonicalPath)
        }
    }

    fun modify(classNames:Set<String> , classPool:ClassPool ) {
        project.logger.error("modify")
        classNames.map { classPool.getCtClass(it) }
//                .findAll{ it.hasAnnotation(Logging.class) }
                .forEach {
                    //            val annotation = (Logging)it.getAnnotation(Logging.class)
//            val tag = annotation.value()

                    it.declaredMethods.filter {
                        project.logger.error("method"+it)
                        !Modifier.isNative(it.getModifiers())
                        && !Modifier.isInterface(it.getModifiers())
                        && !Modifier.isAbstract(it.getModifiers())
                    }.forEach {
                        project.logger.error("filtered method"+it)
                        val startLog = StringBuilder()
                        startLog.append("StringBuilder sb = new StringBuilder(\"(\");")
                                .append("for(int i = 0; i < \$args.length; i++) {")
                                .append("    if (\$args[i] != null) {")
                                .append("        sb.append(\$args[i].toString())")
                                .append("          .append(\",\");")
                                .append("    }")
                                .append("}")
                                .append("sb.append(\")\");")
                                .append("android.util.Log.v(\"valault\", \"${it.getLongName()}\" + sb.toString());")

                        it.insertBefore(startLog.toString())

                        val resultLog = StringBuilder()
                        val returnValue = if (it.getReturnType().getName() == "void") {
                            "\"void\""
                        } else if (it.getReturnType().isPrimitive()) {
                            "\"returns \" + \$_"
                        } else {
                            resultLog.append("String resultValue = \"empty\";")
                                    .append("if (\$_ != null) resultValue = \$_.toString();")
                            "\"returns \" + resultValue"
                        }

                        resultLog.append("android.util.Log.v(\"valault\", \"${it.getLongName()} \" + $returnValue);")

                        it.insertAfter(resultLog.toString())

                    }
                }
    }

    fun getClassPool(inputs: Collection<TransformInput>): ClassPool {
        val classPool = ClassPool(null)
        classPool.appendSystemPath()
        classPool.appendClassPath(LoaderClassPath(this::class.java.getClassLoader()))

        inputs.forEach {
            it.directoryInputs.forEach {
                classPool.appendClassPath(it.file.absolutePath)
            }

            it.jarInputs.forEach {
                classPool.appendClassPath(it.file.absolutePath)
            }
        }

        project.baseExtention()?.bootClasspath?.forEach {
            val path = it.absolutePath
                    classPool.appendClassPath(path)
        }

        return classPool
    }

    fun getClassNames(inputs: Collection<TransformInput>): Set<String> {
        val classNames = HashSet<String>()

        inputs.forEach() {
            it.directoryInputs.forEach {
                val dirPath = it.file.absolutePath
                it.file.walkTopDown().forEach {
                    if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        val className =
                                it.absolutePath.substring(dirPath.length + 1, it.absolutePath.length - 6)
                                        .replace(File.separatorChar, '.')
                        classNames.add(className)
                    }
                }
            }
        }
        return classNames
    }
}
