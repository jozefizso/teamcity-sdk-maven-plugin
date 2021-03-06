package org.jetbrains.teamcity.maven.sdk

/**
 * Created by Nikita.Skvortsov
 * date: 24.03.2014.
 */


import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.FileFilterUtils
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import java.io.File
import java.io.IOException


@Mojo(name = "init", aggregator = true)
public class InitTeamCityMojo() : AbstractTeamCityMojo() {
    override fun doExecute() {
        log.info("Init finished")
    }
}

@Mojo(name = "start", aggregator = true)
public class RunTeamCityMojo() : AbstractTeamCityMojo() {

    @Parameter( defaultValue = "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=10111 -Dteamcity.development.mode=true", property = "serverDebugStr", required = true)
    private var serverDebugStr: String = ""

    @Parameter( defaultValue = "-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=10112", property = "agentDebugStr", required = true)
    private var agentDebugStr: String = ""


    override fun doExecute() {
        val effectiveDataDir = uploadPluginAgentZip()

        log.info("Starting TC in [${teamcityDir?.absolutePath}]")
        log.info("TC data directory is [$effectiveDataDir]")

        val procBuilder = ProcessBuilder()
                .directory(teamcityDir)
                .redirectErrorStream(true)
                .command(createRunCommand("start"))

        procBuilder.environment()?.put("TEAMCITY_DATA_PATH", effectiveDataDir)
        procBuilder.environment()?.put("TEAMCITY_SERVER_OPTS", serverDebugStr)
        procBuilder.environment()?.put("TEAMCITY_AGENT_OPTS", agentDebugStr)

        readOutput(procBuilder.start())

        log.info("TeamCity start command issued. Try opening browser at http://localhost:8111")
    }
}

@Mojo(name = "stop", aggregator = true)
public class StopTeamCityMojo() : AbstractTeamCityMojo() {
    override fun doExecute() {
        log.info("Stopping TC in [${teamcityDir?.absolutePath}]")
        val procBuilder = ProcessBuilder()
                .directory(teamcityDir)
                .redirectErrorStream(true)
                .command(createRunCommand("stop"))
        readOutput(procBuilder.start())
    }
}

@Mojo(name = "reloadResources", aggregator = true)
public class ReloadJSPMojo() : AbstractTeamCityMojo() {
    override fun doExecute() {
        val artifactId = project?.artifactId!!
        val sourceJspDir = File("$artifactId-server/src/main/resources/buildServerResources")
        val targetJspDir = File(teamcityDir, "webapps/ROOT/plugins/$artifactId")
        log.info("Trying to cleanup existing resources in $targetJspDir")
        try {
            FileUtils.cleanDirectory(targetJspDir)
        } catch (e: IOException) {
            log.warn("Failed to clean existing resource. Some old files may have left. Error: ${e.message}")
        }
        log.info("Trying to copy jsp pages from $sourceJspDir to  $targetJspDir")
        FileUtils.copyDirectory(sourceJspDir, targetJspDir, FileFilterUtils.trueFileFilter())
    }
}

@Mojo(name = "reload", aggregator = true)
public class ReloadPluginMojo() : AbstractTeamCityMojo() {
    override fun doExecute() {
        uploadPluginAgentZip()
        log.info("Plugin uploaded. Wait for TeamCity agent to upgrade.")
    }
}
