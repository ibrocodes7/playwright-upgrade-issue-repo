#!groovy
@Library('ts-common@master')
import groovy.json.JsonOutput
def ts = new com.example.ts()

node('devdocker') {
    try {
        ts.checkoutScaligent()
        ts.tsBuild(180) {
            pipeline(ts)
        }
    } catch(Exception e) {
        currentBuild.result = "FAILURE"
        println(e)
        throw e
    }
}

def pipeline(ts) {
    def toolchainVolume = ts.getToolchainVolume()
    def sconsCacheVolume = ts.getSconsCacheVolume()
   stage('Build Devdocker Jenkins Image') {
        devDocker = ts.getDevDocker();
    }
    currentBuild.result = "SUCCESS"
    devDocker.inside("${toolchainVolume} ${sconsCacheVolume} --privileged -v /tmp:/tmp") {
      stage('Run npm run lint for playwright dir') {
                sh script: 'cd blink-playwright &&
                            npm ci && npx playwright test && npm run lint
                            && npx playwright test tests/demo/demo/demo.test.ts --config demo.config.ts'
        }
    }
}
