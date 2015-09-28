package grails.plugins.reloadconfig

import grails.plugins.GrailsPlugin
import grails.util.Environment
import groovy.util.logging.Slf4j
import org.grails.config.PropertySourcesConfig
import org.grails.config.yaml.YamlPropertySourceLoader
import org.springframework.core.io.FileSystemResource

@Slf4j
class ReloadConfigService {
	def pluginManager
	def grailsApplication
	List files
	Date lastTimeChecked
	Date lastTimeReloaded
	ReloadableTimer timer
	Boolean automerge
	Boolean notifyWithConfig

	// Notify plugins list - add external-config-reload automatically	
	private def plugins
	public void setPlugins(def pluginList) {
		this.plugins = pluginList ?: []
		if (!this.plugins.contains("external-config-reload"))
			this.plugins << "external-config-reload"
	}

    def notifyPlugins(List changedFiles=null) {
		log.trace("Notifying ${plugins.size()} plugins${changedFiles?' of changed files '+changedFiles:''}")
		plugins.each { plugin ->
			log.debug("Firing onConfigChange event for plugin ${plugin}")
			pluginManager.getGrailsPlugin(plugin)?.notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE,
					notifyWithConfig ? grailsApplication.config : changedFiles
			)
		}
    }
	
	def checkNow(boolean forceReload = false) {
		log.trace("Check now triggered")

		def changed = []
		files?.each { String fileName ->
			if (fileName.contains("file:"))
				fileName = fileName.substring(fileName.indexOf(':')+1)
			File configFile = new File(fileName).absoluteFile
			log.trace("Checking external config file location ${configFile} for changes since ${lastTimeChecked}...")

			if (!automerge) {
				log.debug("Not performing auto merge of ${configFile} due to configuration")
				return
			}

			if (!configFile.exists()) {
				log.warn("File ${configFile} does not exist, cannot reload")
				return
			}

			if (forceReload || configFile.lastModified() > lastTimeChecked.time) {
				if (forceReload) {
					log.info("Forcing reload of configuration")
				} else if (configFile.lastModified() > lastTimeChecked.time) {
					log.info("Detected changed configuration in ${configFile.name}, reloading configuration")
				}

				try {
					ConfigSlurper configSlurper = new ConfigSlurper(Environment.getCurrent().getName())
					String fileExtension = getFileExtension(fileName)
					if (fileExtension == "properties") {
						def props = new Properties()
						configFile.withInputStream { stream ->
							props.load(stream)
						}
						grailsApplication.config.merge(configSlurper.parse(props))
					} else if (fileExtension in ['yml', 'yaml']) {
						def resource = new FileSystemResource(configFile)
						def mapPropertySource = new YamlPropertySourceLoader().load(fileName, resource, null)
						grailsApplication.config.merge(new PropertySourcesConfig(mapPropertySource.getSource()))

					} else {
						grailsApplication.config.merge(configSlurper.parse(configFile.text))
					}
				} catch (Throwable e) {
					log.error("Failed parsing and merging config file ${configFile} changes", e)
				}
				changed << configFile
			}
		}
		
		// Reset last checked date
		lastTimeChecked = new Date()
		
		// Notify plugins
		if (changed || forceReload) {
			lastTimeReloaded = new Date();
			notifyPlugins(changed);
		}
	}

	private static String getFileExtension(String fileName) {
		fileName?.toLowerCase()?.tokenize('.')[-1]
	}
}

