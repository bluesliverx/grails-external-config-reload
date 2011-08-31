import groovy.util.ConfigObject;
import org.codehaus.groovy.grails.commons.*
import grails.plugins.reloadconfig.*
import grails.util.Environment;

class ExternalConfigReloadGrailsPlugin {
    // the plugin version
    def version = "0.4-SNAPSHOT"
    
	// the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2 > *"
    
	// the other plugins this plugin depends on
    def dependsOn = [quartz:"0.4.2"]
	def loadAfter = ["core", "quartz"]
	
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
			"test-config.groovy",
    ]
	
	def watchedResources = [
		"file:./grails-app/jobs/**/ConfigWatcherJob.groovy",
	]

	// Release information
	def license = "APACHE"
	def organization = [ name:"Adaptive Computing", url:"http://adaptivecomputing.com" ]
	def issueManagement = [ system:"GitHub", url:"http://github.com/adaptivecomputing/grails-external-config-reload/issues" ]
	def scm = [ url:"http://github.com/adaptivecomputing/grails-external-config-reload" ]
	
    def author = "Brian Saville"
    def authorEmail = "bsaville@adaptivecomputing.com"
    def title = "External Configuration Reload"
    def description = '''\
This plugin will poll for changes to external configuration files (files added to grails.config.locations), reload
the configuration when a change has occurred, and notify specified plugins by firing the onConfigChange event in each.
This plugin, unlike reloadable config (http://www.grails.org/plugin/reloadable-config), depends on Quartz.

Please note: No warranty is implied or given with this plugin.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/external-config-reload"

    def doWithWebDescriptor = { xml ->
    }

    def doWithSpring = {
    }

    def doWithDynamicMethods = { ctx ->
    }

    def doWithApplicationContext = { applicationContext ->
		ReloadConfigUtility.configureWatcher(application)
    }

    def onChange = { event ->
        // Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
		if (event.source && event.source.name=="grails.plugins.reloadconfig.ConfigWatcherJob") {
			ReloadConfigUtility.configureWatcher(event.application, true)
		}
    }

    def onConfigChange = { event ->
		// Reload config watcher - for development help only
		ReloadConfigUtility.configureWatcher(event.application, true, event.ctx)
    }
}
