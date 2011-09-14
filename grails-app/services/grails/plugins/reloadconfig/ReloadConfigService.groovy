package grails.plugins.reloadconfig

import org.codehaus.groovy.grails.plugins.GrailsPlugin

class ReloadConfigService {
	def pluginManager
	def grailsApplication
	List files
	Date lastTimeChecked

	// Notify plugins list - add external-config-reload automatically	
	private def plugins
	public void setPlugins(def pluginList) {
		this.plugins = pluginList ?: []
		if (!this.plugins.contains("external-config-reload"))
			this.plugins << "external-config-reload"
	}

    def notifyPlugins() {
		log.debug("Notifying ${plugins.size()} plugins")
		plugins.each { plugin ->
			log.debug("Firing onConfigChange event for plugin ${plugin}")
			pluginManager.getGrailsPlugin(plugin)?.notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE, null)
		}
    }
	
	def checkNow() {
		log.debug("Check now triggered")
		
		// Check for changes
		def changed = false
		files?.each { String fileName ->
			if (fileName.contains("file:"))
				fileName = fileName.substring(fileName.indexOf(':')+1)
			File configFile = new File(fileName).absoluteFile
			log.debug("Checking external config file location ${configFile} for changes since ${lastTimeChecked}...")
			if (configFile.exists() && configFile.lastModified()>lastTimeChecked.time) {
				log.debug("Detected changed configuration, reloading configuration")
				grailsApplication.config.merge(new ConfigSlurper().parse(configFile.text))
				changed = true
			}
		}
		
		// Reset last checked date
		lastTimeChecked = new Date()
		
		// Notify plugins
		if (changed) {
			notifyPlugins();
		}
	}
	
	def reloadNow() {
		log.debug("Reload now triggered")
		files?.each { String fileName ->
			if (fileName.contains("file:"))
				fileName = fileName.substring(fileName.indexOf(':')+1)
			File configFile = new File(fileName).absoluteFile
			if (configFile.exists()) {
				log.debug("Reloading ${configFile} manually")
				grailsApplication.config.merge(new ConfigSlurper().parse(configFile.text))
			} else {
				log.warn("File ${configFile} does not exist, cannot reload")
			}
		}
		lastTimeChecked = new Date();
		notifyPlugins();
	}
}
