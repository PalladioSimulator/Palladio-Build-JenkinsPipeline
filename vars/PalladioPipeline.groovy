def call(body) {
    AbstractMDSDToolsDSLPipeline {
        agent_label = 'docker'

        buildWithMaven {
            version = '3'
            jdkVersion = 11
            settingsId = 'fba2768e-c997-4043-b10b-b5ca461aff55'
            goal = 'clean verify'
        }

        constraintBuild {
            timeLimitMinutes = 30
            ramLimit = '4G'
            hddLimit = '20G'
        }
        
        skipDeploy (['master', 'main'].find{it == "${this.BRANCH_NAME}"} == null)
        skipNotification (['master', 'main'].find{it == "${this.BRANCH_NAME}"} == null)
        
        deployUpdatesiteSshName 'web'
        deployUpdatesiteRootDir '/home/sftp/data'
        deployUpdatesiteSubDir (['master', 'main'].find{it == "${this.BRANCH_NAME}"} != null ? 'nightly': "branches/${this.BRANCH_NAME}")
        deployUpdatesiteProjectDir constructDeployUpdatesiteProjectDir(this.scm.userRemoteConfigs[0].url).toLowerCase()

        createCompositeUpdatesiteScriptFileId '608d94a5-3ba4-4601-9a07-46fab5b37752'
             
        notifyDefault 'cGFsbGFkaW8tYnVpbGRAaXJhLnVuaS1rYXJsc3J1aGUuZGU='

        body.delegate = delegate
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body()
    }
}

def constructDeployUpdatesiteProjectDir(scmUrl) {
    def pattern = /^.*[:\/]([^\/]+)\/([^\/]+)\.git$/
    def matcher = (scmUrl =~ pattern).findAll()
    if (matcher[0][1] == 'PalladioSimulator') {
        return matcher[0][2]
    }
    return matcher[0][1] + '/' + matcher[0][2]
}
