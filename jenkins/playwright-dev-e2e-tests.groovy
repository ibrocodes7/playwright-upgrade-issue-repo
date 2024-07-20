#!groovy
@Library('ts-common@master')

def ts = new com.example.ts()
def cloudName = params.CLOUD ?: 'ts-k8s-cicd'
def nodeName = params.POD_TEMPLATE ?: 'jenkins-k8s-latest'
def tpch_reservation = null
def browser_reservation = null

def upstreamList = currentBuild.getBuildCauses().upstreamProject
def upstreamIs9_8 = upstreamList && upstreamList[0] == "release-sanity-tests-9.8"

// If backendips are provided we need to use the right pod template as
// POD_TEMPLATE may not be set correctly
if(cloudName.equalsIgnoreCase('eks')) {
    nodeName = ts.getPodTemplate(cloudName)
}

try{
    ts.getK8SSlave(nodeName) {
        ws ('/home/hudson/inmem_workspace') {
            // Reset job display name
            ts.setDisplayName()
            println "Starting blink-on-demand-tests pipeline"
            ts.checkoutScaligent()
            def common = load 'blink/jenkins/blink-common.groovy'

            def extraCmdParams = ''
            def resetMinPassCount = false
            def keepTestResults = false
            if(params.TESTS_XML_FILE) {
                // Put this file in test/results/shard-000 so protractorFlake picks it up automatically
                sh "mkdir -p blink/test/results/shard-000"
                dir('blink/test/results/shard-000') {
                    def xml_files = params.TESTS_XML_FILE.split(',')
                    for (def i = 0; i < xml_files.size(); i++) {
                        sh "wget -O - ${xml_files[i]} >./protractor-e2e-attempt-1-${i}.xml"
                    }
                    // Start at attempt 2 as otherwise protractorFlake won't pick this up
                    extraCmdParams = " --test-attempt=${i-1}"
                    keepTestResults = true
                }
                resetMinPassCount = true
            } else if(params.TEST_SPECS) {
                extraCmdParams = " --spec-file='${params.TEST_SPECS}'"
                // We don't know how many tests are there without processing
                resetMinPassCount = true
            } else if(params.TEST_NAME_FILTER) {
                extraCmdParams = " --test-name-filter='${params.TEST_NAME_FILTER}'"
                // We don't know how many tests are there without processing
                resetMinPassCount = true
            }

            if(params.SPEC_NAME) {
                extraCmdParams += " --spec-name='${params.SPEC_NAME}'"
                resetMinPassCount = true
            }

            if(params.SKIP_VOYAGER_CHECK) {
                extraCmdParams += " --skip_voyager_check=True "
            }

            if(params.TEST_TARGET) {
                extraCmdParams += common.getTargetArgs(params.TEST_TARGET)
            }

            def config = common.getSmokesConfig(params.BROWSER)
            switch(params.TEST_MODE) {
                case 'Full':
                    config = common.getFullModeConfig(params.BROWSER)
                    break
                case 'TPCH':
                    config = common.getPerfTPCHConfig(params.BROWSER)
                    break
                case 'Embrace':
                    config = common.getEmbraceModeConfig(params.BROWSER)
                    break
                case 'XEmbrace':
                    config = common.getXEmbraceModeConfig(params.BROWSER)
                    break
                case 'TIA':
                    config = common.getTiaConfig(params.BROWSER)
                    break
                case 'FDP':
                    config = common.getFDPConfig(params.BROWSER)
                    break
                default:
                    break
            }

            if (params.ENABLE_ORGS) {
                config.runCommand += ' --run-orgs-tests'
            }

            if (!!params.URL_FLAGS) {
                config.runCommand += " --url-flags='${params.URL_FLAGS}'"
            }

            ts.printInfo("runCommand (before playwright): ${config.runCommand}")

            if (!!params.EMBED_MODE) {
                config.runCommand += ' --embed-mode'
            }

            if (!!params.NAVIGATION_TIMEOUT) {
                config.runCommand += " --navigation-timeout='${params.NAVIGATION_TIMEOUT}'"
            }

            if (!!params.ACTION_TIMEOUT) {
                config.runCommand += " --action-timeout='${params.ACTION_TIMEOUT}'"
            }

            if (!!params.TEST_TIMEOUT) {
                config.runCommand += " --test-timeout='${params.TEST_TIMEOUT}'"
            }

            if(params.USE_PLAYWRIGHT) {
                config.enable_playwright_flags = true
            }

            if(!!params.USE_PLAYWRIGHT) {

                if (!!params.RERUN_ONLY_FAILED_TESTS) {
                    try {

                        step([$class: 'CopyArtifact', projectName: 'blink-playwright-dev-e2e-tests', filter: 'blink-playwright/test-map.json', selector: [$class: 'SpecificBuildSelector', buildNumber: "${params.RERUN_BUILD_NUMBER}"]])
                        step([$class: 'CopyArtifact', projectName: 'blink-playwright-dev-e2e-tests', filter: 'allure-report/data/suites.json', selector: [$class: 'SpecificBuildSelector', buildNumber: "${params.RERUN_BUILD_NUMBER}"]])

                        sh "cd blink-playwright && node rerun-extractor.js"

                        archiveArtifacts artifacts: 'blink-playwright/test-map.json', allowEmptyArchive: true
                        archiveArtifacts artifacts: 'allure-report/data/suites.json', allowEmptyArchive: true
                    } catch(error) {
                        ts.printError('Failed to generate rerun JSON map.')
                    }
                }

                sh "mkdir -p blink-playwright/test-results/"
                // This script is run under blink directory
                def preTestRunCommand = !!params.RERUN_ONLY_FAILED_TESTS ? 'cd ../blink-playwright' : 'cd ../blink-playwright && node test-extractor.js'

            if (!params.RERUN_ONLY_FAILED_TESTS) {

                if (!!params.TEST_PATH && TEST_PATH.trim().size() > 0) {
                    preTestRunCommand += " '${TEST_PATH}'"
                } else {
                    preTestRunCommand += " ''"
                }

                if (!!params.TEST_MODE && TEST_MODE.trim().size() > 0) {
                    preTestRunCommand += " '${TEST_MODE}'"
                } else {
                    preTestRunCommand += " ''"
                }

                if (!!params.GREP_INVERT && GREP_INVERT.trim().size() > 0) {
                    preTestRunCommand += " '${GREP_INVERT}'"
                } else {
                    preTestRunCommand += " ''"
                }
            }

                config.preTestRunCommand = preTestRunCommand
                // config.preTestRunCommand += 'cd ../blink-playwright && PLAYWRIGHT_BROWSERS_PATH=./.cache/ms-playwright npx playwright install'
                ts.printInfo("preTestRunCommand: ${config.preTestRunCommand}")

                def tokens = config.runCommand.split(' ')
                tokens -= '--prepare'
                config.runCommand = tokens.join(' ')
                println(config.runCommand)

                config.runCommand += ' --run-cmd=../blink-playwright/scripts/playwright-wrapper.sh'
                config.runCommand += ' --spec-map-file=../blink-playwright/test-map.json'
                config.runCommand += ' --test-path=../blink-playwright'
                config.runCommand += ' --use-playwright'
                ts.printInfo("runCommand (after playwright): ${config.runCommand}")
            }

            // Setup taking longer than expected
            def timeout = config.runCommandTimeout + 120 ?: 180
            config.blinkPackage = params.BLINK_PACKAGE ?: null
            config.keepTestResults = keepTestResults
            config.extraCmdParams = extraCmdParams

            if(params.BACKEND_IP) {
                config.noPrepare = true
                config.noCleanup = true
                config.clusterIps = params.BACKEND_IP.split(',')
                config.clusterSize = config.clusterIps.size()
                config.useHttps = params.BACKEND_HTTPS
                config.resourceID = 'xxx' // Not relevant if IP specified
            }

            if(params.BROWSER_IP) {
                config.browserIps = params.BROWSER_IP.split(',')
                config.extraCmdParams += ' --browser-hosts=' + params.BROWSER_IP
                config.browserResourceID = null // Not clean up required if IPs specified
                config.noBrowserResourceCleanup = true
            }

            if(params.RUNTIME_PARAMS) {
                config.extraCmdParams += " ${params.RUNTIME_PARAMS}"
            }

            if(params.TEST_MODE.equals('FDP') && params.NUM_CANDIDATES) {
                config.extraCmdParams +=  " --num_flaky_candidates=${params.NUM_CANDIDATES} "
            }

            if(resetMinPassCount) {
                config.minPassCount = 1
            }

            if(params.ENCODE_QUERY_PARAMS) {
                config.extraCmdParams +=  " --encode_query_params=${params.ENCODE_QUERY_PARAMS} "
            }

            if(params.ENABLE_GET_REQUEST_VALIDATOR) {
                config.extraCmdParams += " --enable_get_request_validator=${params.ENABLE_GET_REQUEST_VALIDATOR} "
            }

            ts.tsBuild(timeout) {
                println "Browser used to run test is  ${params.BROWSER}."
                common.vncPipeline(ts, config, true, cloudName, tpch_reservation, browser_reservation)
            }
        }
    }
} catch (Exception e) {
        currentBuild.result = "FAILURE"
} finally {
    if (upstreamIs9_8) {
        if(currentBuild.result == "FAILURE" || currentBuild.result == "ABORT"){
            build job: 'app-ci-auto-triage',
                parameters: [
                    string(name: 'PIPELINE_NAME', value: env.JOB_NAME),
                    string(name: 'BUILD_NUM', value: env.BUILD_NUMBER),
                    string(name: 'SLACK_THREAD', value: env.NOTIFICATIONTHREADID)
                ],
                propagate: false,
                wait: false
        }
    }
}