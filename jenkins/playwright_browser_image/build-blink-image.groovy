#!groovy
@Library('ts-common@master')

def ts = new com.example.ts()

node('docker-builder') {
    ts.setDisplayName()
    ts.checkoutScaligent()
    ts.tsBuild(300) {
        stage('Build blink image') {
            dir('blink-playwright/jenkins/playwright_browser_image') {
                def command = "./build.sh"
                command += params.BASE_IMAGE ? " -b ${params.BASE_IMAGE}" : ""
                command += params.IMAGE_TAG ? " -i ${params.IMAGE_TAG}" : ""

                def outputFile = "blinkBuildOutput.log"

                sh "export PW_VERSION=${params.PLAYWRIGHT_VERSION} && ${command} 2>&1 | tee ${outputFile}"

                def openFile = readFile outputFile
                def lines = openFile.readLines()
                def image_name = lines.get(lines.size()-1)
                println("Image name is: " + image_name)

                if (params.ADD_IMAGE_TO_NEBULA) {
                    def baseImage = image_name.split(":")[0]
                    def tag = image_name.split(":")[1]
                    def imageType = 'browser'
                    ts.addImageToNebula(baseImage, tag, imageType)
                }
            }
        }
    }
}