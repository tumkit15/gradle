/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.gradlebuild.distributed

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.gradlebuild.distributed.model.BuildInvocation
import org.gradle.gradlebuild.distributed.model.Stage
import java.net.URL
import java.util.UUID
import javax.inject.Inject


open class RunDistributedStageTask @Inject constructor(@Internal val stage: Stage) : DefaultTask() {

    private
    var jobNumber = 0

    @TaskAction
    fun runDistributed() {
        println("Executing stage on CI: ${stage.name} - ${stage.description}")

        val jobsStarted = mutableListOf<Int>()
        stage.buildInvocation.forEach { invocation ->
            invocation.findTasksForInvocation(project).forEach { buildTypeTaskList ->
                jobsStarted.add(triggerJob(invocation, buildTypeTaskList))
            }
        }

        waitForJobsToFinish(jobsStarted)
    }

    private
    fun triggerJob(invocation: BuildInvocation, buildTypeTaskList: List<String>): Int {
        val commit = "jjohannes/experiment/distributed" //TODO get commit ID from git and check that working copy is not dirty
        val jobURL = "http://localhost:8080/job/${invocation.buildEnvironment.asEnvironmentSpecificName("GradleWorker")}"

        val getJobNumber = URL("$jobURL/lastBuild/buildNumber")
        val trigger = URL("$jobURL/buildWithParameters?tasks=${buildTypeTaskList.joinToString("%20")}&commit=$commit")
        println("  Starting on remote agent: $buildTypeTaskList")

        // TODO actually trigger on Jenkins
        /*
        val lastBuild = getJobNumber.readText().toInt()
        trigger.readText()
        var currentBuild = lastBuild
        while (currentBuild == lastBuild) {
            Thread.sleep(100)
            currentBuild = getJobNumber.readText().toInt()
        }

        val statusCheck = URL("$jobURL/$currentBuild/api/xml?depth=0")
        while (statusCheck.readText().contains("<building>true</building>")) {
            Thread.sleep(500)
        }
        return currentBuild;
        */

        jobNumber++
        return jobNumber
    }

    private
    fun waitForJobsToFinish(jobsStarted: List<Int>) {
        jobsStarted.forEach {
            // TODO actually wait for job to start and to finish
            Thread.sleep(300)
            println("  Finished $it - https://scans.gradle.com/s/${UUID.randomUUID().toString().substring(0,6)}")
        }
    }
}
