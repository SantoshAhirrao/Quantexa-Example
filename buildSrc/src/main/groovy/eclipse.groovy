/*Creates a configuration file to get Eclipse to use the correct Scala Version*/

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

class EclipseConfigureScalaCompilerTask extends DefaultTask {

    String projectDirectory
    String scalaVersion

    public void writeToFile(def projectDirectory, def directory, def fileName, def extension, def infoList) {
        new File("$projectDirectory/$directory/$fileName$extension").withWriter { out ->
            out.println infoList
        }
    }

    @TaskAction
    def createScalaCompilerCorePrefs(){
        String scalaOptionsString = """P=
scala.compiler.useProjectSettings=true
target=jvm-1.8
scala.compiler.installation=${scalaVersion}
scala.compiler.additionalParams=-Xsource:${scalaVersion}
"""
        writeToFile("${projectDirectory}","./.settings","org.scala-ide.sdt.core",".prefs",scalaOptionsString)
    }
}
