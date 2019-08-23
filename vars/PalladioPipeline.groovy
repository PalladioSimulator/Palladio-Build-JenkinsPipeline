def call(body) {
    AbstractMDSDToolsDSLPipeline {
        agent_label = 'docker'

        buildWithMaven {
            version = '3.6.0'
            jdkVersion = 11
        }

        constraintBuild {
            timeLimitMinutes = 30
            ramLimit = '4G'
            hddLimit = '20G'
        }
        
        skipDeploy ("${this.BRANCH_NAME}" != 'master')
        skipNotification ("${this.BRANCH_NAME}" != 'master')
        
        deployUpdatesiteSshName 'web'
        deployUpdatesiteRootDir '/home/sftp/data'
        deployUpdatesiteSubDir ("${this.BRANCH_NAME}" == 'master' ? 'nightly': "branches/${this.BRANCH_NAME}")
        deployUpdatesiteProjectDir this.scm.userRemoteConfigs[0].url.replaceFirst(/^.*\/([^\/]+?).git$/, '$1').toLowerCase()
             
        notifyDefault 'cGFsbGFkaW8tYnVpbGRAaXJhLnVuaS1rYXJsc3J1aGUuZGU='

        body.delegate = delegate
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body()
    }
}