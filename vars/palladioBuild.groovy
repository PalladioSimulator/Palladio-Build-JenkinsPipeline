import org.codehaus.groovy.util.ReleaseInfo

def call(body) {

	final JAVA_TOOL_NAME = 'JDK 1.8'
	final MAVEN_TOOL_NAME = 'Maven-3.5.4'
	final SSH_CONFIG_NAME = 'SDQ Webserver Eclipse Update Sites'
	final GIT_BRANCH_PATTERN = 'master'
	
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	node {
		def mandatoryParameters = ['gitUrl', 'webserverDir', 'updateSiteLocation']
		for (mandatoryParameter in mandatoryParameters) {
			if (!config.containsKey(mandatoryParameter) || config.get(mandatoryParameter).toString().trim().isEmpty()) {
				error "Missing mandatory parameter $mandatoryParameter"
			}			
		}
		
		boolean doReleaseBuild = params.DO_RELEASE_BUILD.toString().toBoolean()
		String releaseVersion = params.RELEASE_VERSION		
		if (doReleaseBuild && (releaseVersion == null || releaseVersion.trim().isEmpty())) {
			error 'A release build requires a proper release version.'
		}
		
		if (doReleaseBuild) {
			currentBuild.rawBuild.keepLog(true)
		}
		
		deleteDir()

		try {
			stage ('Clone') {
				git (
					url: "${config.gitUrl}",
					branch: GIT_BRANCH_PATTERN
					)
			}
			stage ('Build') {
				withEnv([
					"JAVA_HOME=${ tool JAVA_TOOL_NAME }",
					"PATH+MAVEN=${tool MAVEN_TOOL_NAME}/bin:${env.JAVA_HOME}/bin"
				]) {
					genericSh "mvn clean verify"
				}
			}
			stage ('Deploy') {
				String absoluteWebserverDir = "/var/www/html/eclipse/${config.webserverDir}"

				sshPublisher(
						failOnError: true,
						publishers: [
							sshPublisherDesc(
							configName: SSH_CONFIG_NAME,
							transfers: [
								sshTransfer(
								execCommand:
								"mkdir -p $absoluteWebserverDir/nightly &&" +
								"rm -rf $absoluteWebserverDir/nightly/*" 
								),
								sshTransfer(
								sourceFiles: "${config.updateSiteLocation}/**/*",
								removePrefix: "${config.updateSiteLocation}",
								remoteDirectory: "$absoluteWebserverDir/nightly/"
								)
							]
							)
						]
						)

				if (doReleaseBuild) {
					sshPublisher(
							failOnError: true,
							publishers: [
								sshPublisherDesc(
								configName: SSH_CONFIG_NAME,
								transfers: [
									sshTransfer(
									execCommand:
									"rm -rf $absoluteWebserverDir/releases/latest &&" +
									"rm -rf $absoluteWebserverDir/releases/$releaseVersion &&" +
									"mkdir -p $absoluteWebserverDir/releases/$releaseVersion &&" +
									"cp -a $absoluteWebserverDir/nightly/* $absoluteWebserverDir/releases/$releaseVersion/ &&" +
									"ln -s $absoluteWebserverDir/releases/$releaseVersion $absoluteWebserverDir/releases/latest"
									),
									sshTransfer(
									sourceFiles: "${config.updateSiteLocation}/**/*",
									removePrefix: "${config.updateSiteLocation}",
									remoteDirectory: "$absoluteWebserverDir/nightly/"
									)
								]
								)
							]
							)
				}
			}
			stage ('Archive') {
				archiveArtifacts "${config.updateSiteLocation}/**/*"
				publishHTML([
					allowMissing: false,
					alwaysLinkToLastBuild: false,
					keepAll: false,
					reportDir: "${config.updateSiteLocation}/javadoc",
					reportFiles: 'index.html',
					reportName: 'JavaDoc',
					reportTitles: ''
				])
			}
			stage ('QualityMetrics') {
				checkstyle([
					pattern: '**/target/checkstyle-result.xml'
				])
				junit([
					testResults: '**/surefire-reports/*.xml',
					allowEmptyResults: true
				])
				jacoco([
					execPattern: '**/target/*.exec',
					classPattern: '**/target/classes',
					sourcePattern: '**/src,**/src-gen,**/xtend-gen',
					inclusionPattern: '**/*.class',
					exclusionPattern: '**/*Test*.class'
				])
			}
		} catch (err) {
			currentBuild.result = 'FAILED'
			throw err
		}
	}
}

def genericSh(cmd) {
	if (isUnix()) {
		sh cmd
	}
	else {
		bat cmd
	}
}