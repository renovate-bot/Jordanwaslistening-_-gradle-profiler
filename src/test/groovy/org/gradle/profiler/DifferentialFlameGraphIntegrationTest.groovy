package org.gradle.profiler

import spock.lang.Requires
import spock.lang.Unroll

@Unroll
class DifferentialFlameGraphIntegrationTest extends AbstractProfilerIntegrationTest implements FlameGraphFixture {
    @Requires({ !OperatingSystem.isWindows() })
    def "generates differential flame graphs with #profiler"() {
        given:
        instrumentedBuildScript()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--gradle-version", minimalSupportedGradleVersion, "--profile", profiler, "assemble")

        then:
        logFile.find("<daemon: true").size() == 8
        logFile.find("<invocations: 3>").size() == 2

        and:
        assertGraphsGenerated()
        assertDifferentialGraphsGenerated([latestSupportedGradleVersion, minimalSupportedGradleVersion])

        where:
        profiler << ["async-profiler", "jfr"]
    }

    @Requires({ !OperatingSystem.isWindows() })
    def "generates differential flame graphs with #profiler for scenario file"() {
        given:
        instrumentedBuildScript()
        def scenarioFile = file("performance.scenarios")
        scenarioFile << """
            upToDate {
                tasks = ["assemble"]
            }
        """.stripIndent()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion, "--gradle-version", minimalSupportedGradleVersion,
            "--scenario-file", scenarioFile.absolutePath,
            "--profile", profiler,
            "upToDate"
        )

        then:
        logFile.find("<daemon: true").size() == 8
        logFile.find("<invocations: 3>").size() == 2

        and:
        assertGraphsGenerated('upToDate')
        assertDifferentialGraphsGenerated(['upToDate'], [latestSupportedGradleVersion, minimalSupportedGradleVersion])


        where:
        profiler << ["async-profiler", "jfr"]
    }

    @Requires({ !OperatingSystem.isWindows() })
    def "generates differential flame graphs with #profiler for cross-build scenarios"() {
        given:
        instrumentedBuildScript()
        def scenarioFile = file("performance.scenarios")
        scenarioFile << """
            upToDate {
                tasks = ["assemble"]
            }
            help {
                tasks = ["help"]
            }
        """.stripIndent()

        when:
        new Main().run("--project-dir", projectDir.absolutePath, "--output-dir", outputDir.absolutePath, "--gradle-version", latestSupportedGradleVersion,
            "--scenario-file", scenarioFile.absolutePath,
            "--profile", profiler,
            "upToDate", "help"
        )

        then:
        logFile.find("<daemon: true").size() == 7
        logFile.find("<invocations: 3>").size() == 2

        and:
        assertGraphsGenerated(['upToDate', 'help'])
        assertDifferentialGraphsGenerated(['upToDate', 'help'], [latestSupportedGradleVersion])


        where:
        profiler << ["async-profiler", "jfr"]
    }
}
