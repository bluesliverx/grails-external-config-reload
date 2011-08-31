package grails.plugins.reloadconfig

class ConfigWatcherJob {
	static Boolean initialRun = true
	def grailsApplication
	def reloadConfigService
	
    def execute(context) {
		// Prevents cyclic reloads when a reload is triggered
		if (initialRun) {
			initialRun = false
			return
		}
		
		def changed = false
		def interval = context.mergedJobDataMap.interval
		// Once in awhile a null interval seems to be sent, ignore these if they come
		if (!interval)
			return
			
		// Check for changes
		context.mergedJobDataMap.files?.each { String fileName ->
			if (fileName.contains("file:"))
				fileName = fileName.substring(fileName.indexOf(':')+1)
			File configFile = new File(fileName).absoluteFile
			log.debug("Checking external config file location ${configFile} for changes in the last ${interval} ms ...")
			if (configFile.exists() && (new Date().time-configFile.lastModified())<=interval) {
				log.debug("Detected changed configuration, reloading configuration")
				grailsApplication.config.merge(new ConfigSlurper().parse(configFile.text))
				changed = true
			}
		}
		if (changed) {
			reloadConfigService.notifyPlugins();
		}
		
    }
}
