package grails.plugins.reloadconfig

import org.codehaus.groovy.grails.plugins.GrailsPlugin

class ReloadConfigService {
	def pluginManager

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
}
