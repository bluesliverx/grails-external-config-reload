package grails.plugins.reloadconfig

import org.codehaus.groovy.grails.plugins.GrailsPlugin
import grails.util.Environment

class ReloadConfigService {
	def pluginManager
	def grailsApplication
	List files
	Date lastTimeChecked
	ReloadableTimer timer

	// Notify plugins list - add external-config-reload automatically	
	private def plugins
	public void setPlugins(def pluginList) {
		this.plugins = pluginList ?: []
		if (!this.plugins.contains("external-config-reload"))
			this.plugins << "external-config-reload"
	}

    def notifyPlugins(List changedFiles=null) {
		log.debug("Notifying ${plugins.size()} plugins${changedFiles?' of changed files '+changedFiles:''}")
		plugins.each { plugin ->
			log.debug("Firing onConfigChange event for plugin ${plugin}")
			pluginManager.getGrailsPlugin(plugin)?.notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE, changedFiles)
		}
    }
	
	def checkNow() {
		log.debug("Check now triggered")
		
		// Check for changes
		def changed = []
		files?.each { String fileName ->
			if (fileName.contains("file:"))
				fileName = fileName.substring(fileName.indexOf(':')+1)
			File configFile = new File(fileName).absoluteFile
			log.debug("Checking external config file location ${configFile} for changes since ${lastTimeChecked}...")
			if (configFile.exists() && configFile.lastModified()>lastTimeChecked.time) {
				log.info("Detected changed configuration in ${configFile.name}, reloading configuration")
				grailsApplication.config.merge(new ConfigSlurper(Environment.getCurrent().getName()).parse(configFile.text))
				changed << configFile
			}
		}
		
		// Reset last checked date
		lastTimeChecked = new Date()
		
		// Notify plugins
		if (changed) {
			notifyPlugins(changed);
		}
	}
	
	def reloadNow() {
		log.info("Manual reload of configuration files triggered")
		files?.each { String fileName ->
			if (fileName.contains("file:"))
				fileName = fileName.substring(fileName.indexOf(':')+1)
			File configFile = new File(fileName).absoluteFile
			if (configFile.exists()) {
				log.debug("Reloading ${configFile} manually")
				grailsApplication.config.merge(new ConfigSlurper(Environment.getCurrent().getName()).parse(configFile.text))
			} else {
				log.warn("File ${configFile} does not exist, cannot reload")
			}
		}
		lastTimeChecked = new Date();
		notifyPlugins();
	}
}
