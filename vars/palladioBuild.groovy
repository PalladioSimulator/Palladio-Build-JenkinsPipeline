@Library('MDSD.tools') _

def call(body) {
	
	final MAIL_DEFAULT_RECIPIENT = new String('cGFsbGFkaW8tYnVpbGRAaXJhLnVuaS1rYXJsc3J1aGUuZGU='.decodeBase64())
	final BUILD_IMAGE = 'maven:3.6.0-jdk-11'
	final BUILD_LIMIT_TIME = 40
	final BUILD_LIMIT_RAM = '4G'
	final BUILD_LIMIT_HDD = '20G'
	final SSH_NAME = 'SDQ Webserver Eclipse Update Sites'
	final WEB_ROOT = '/var/www/html/eclipse'
	
	library('MDSD.tools')
	slaveEclipsePipeline(body, SSH_NAME, WEB_ROOT, MAIL_DEFAULT_RECIPIENT, BUILD_IMAGE, BUILD_LIMIT_TIME, BUILD_LIMIT_RAM, BUILD_LIMIT_HDD)

}