package com.github.takahirom.logger

import com.android.SdkConstants
import com.android.build.api.transform.*
import com.android.build.api.transform.QualifiedContent.ContentType
import com.android.build.api.transform.QualifiedContent.DefaultContentType
import com.android.build.api.transform.QualifiedContent.Scope
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import groovy.io.FileType
import javassist.ClassPool
import javassist.LoaderClassPath
import javassist.Modifier
import org.gradle.api.Project

/**
 * from {@url https://github.com/kobito-kaba/TransformApiSample/blob/master/Sample1/logger/src/main/groovy/jp/co/yahoo/sample/logger/LoggerTransformer.groovy}
 */
public class LoggerTransformer extends Transform {

    private Project project

    public LoggerTransformer(Project project) {
        this.project = project
    }

    @Override
    String getName() {
        return 'LoggerTransformer'
    }

    @Override
    Set<ContentType> getInputTypes() {
        return ImmutableSet.<ContentType> of(DefaultContentType.CLASSES)
    }

    @Override
    Set<Scope> getScopes() {
        return Sets.immutableEnumSet(Scope.PROJECT)
    }

    @Override
    Set<Scope> getReferencedScopes() {
        return Sets.immutableEnumSet(Scope.EXTERNAL_LIBRARIES, Scope.PROJECT_LOCAL_DEPS,
                Scope.SUB_PROJECTS, Scope.SUB_PROJECTS_LOCAL_DEPS, Scope.TESTED_CODE)
    }

    @Override
    boolean isIncremental() {
        return false
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        super.transform(transformInvocation)

        def outputProvider = transformInvocation.getOutputProvider()

        def outputDir = outputProvider.getContentLocation('logger',
                getInputTypes(), getScopes(), Format.DIRECTORY)

        def inputs = transformInvocation.getInputs()
        def classNames = getClassNames(inputs)

        def mergedInputs = inputs + transformInvocation.getReferencedInputs()
        ClassPool classPool = getClassPool(mergedInputs)

        modify(classNames, classPool)

        classNames.each {
            def ctClass = classPool.getCtClass(it)
            ctClass.writeFile(outputDir.canonicalPath)
        }
    }

    private static void modify(Set<String> classNames, ClassPool classPool) {
        classNames.collect { classPool.getCtClass(it) }
//                .findAll{ it.hasAnnotation(Logging.class) }
                .each {
//            def annotation = (Logging)it.getAnnotation(Logging.class)
//            def tag = annotation.value()

            it.declaredMethods.findAll {
                !Modifier.isNative(it.getModifiers()) \
                           && !Modifier.isInterface(it.getModifiers()) \
                           && !Modifier.isAbstract(it.getModifiers())
            }.each {
                def startLog = new StringBuilder()
                startLog.append("StringBuilder sb = new StringBuilder(\"(\");")
                        .append("for(int i = 0; i < \$args.length; i++) {")
                        .append("    if (\$args[i] != null) {")
                        .append("        sb.append(\$args[i].toString())")
                        .append("          .append(\",\");")
                        .append("    }")
                        .append("}")
                        .append("sb.append(\")\");")
                        .append("android.util.Log.v(\"default\", \"${it.getLongName()}\" + sb.toString());")

                it.insertBefore(startLog.toString())

                def resultLog = new StringBuilder()
                def returnValue
                if (it.getReturnType().getName() == "void") {
                    returnValue = "\"void\""
                } else if (it.getReturnType().isPrimitive()) {
                    returnValue = "\"returns \" + \$_"
                } else {
                    resultLog.append("String resultValue = \"empty\";")
                            .append("if (\$_ != null) resultValue = \$_.toString();")
                    returnValue = "\"returns \" + resultValue"
                }

                resultLog.append("android.util.Log.v(\"default\", \"${it.getLongName()} \" + $returnValue);")

                it.insertAfter(resultLog.toString())

            }
        }
    }

    private ClassPool getClassPool(Collection<TransformInput> inputs) {
        ClassPool classPool = new ClassPool(null)
        classPool.appendSystemPath()
        classPool.appendClassPath(new LoaderClassPath(getClass().getClassLoader()))

        inputs.each {
            it.directoryInputs.each {
                classPool.appendClassPath(it.file.absolutePath)
            }

            it.jarInputs.each {
                classPool.appendClassPath(it.file.absolutePath)
            }
        }

        project.android.bootClasspath.each {
            String path = it.absolutePath
            classPool.appendClassPath(path)
        }

        return classPool
    }

    static Set<String> getClassNames(Collection<TransformInput> inputs) {
        Set<String> classNames = new HashSet<String>()

        inputs.each {
            it.directoryInputs.each {
                def dirPath = it.file.absolutePath
                it.file.eachFileRecurse(FileType.FILES) {
                    if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                        def className =
                                it.absolutePath.substring(dirPath.length() + 1, it.absolutePath.length() - 6)
                                        .replace(File.separatorChar, '.' as char)
                        classNames.add(className)
                    }
                }
            }
        }
        return classNames
    }
}
