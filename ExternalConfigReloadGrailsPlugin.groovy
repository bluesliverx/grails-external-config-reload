import groovy.util.ConfigObject;
import org.codehaus.groovy.grails.commons.*
import grails.plugins.reloadconfig.*
import grails.util.Environment;

class ExternalConfigReloadGrailsPlugin {
    // the plugin version
    def version = "1.2-SNAPSHOT"
    
	// the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.2 > *"
    
	// the other plugins this plugin depends on
    def dependsOn = [:]
	def loadAfter = ["core", "services"]
	
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp",
			"**/Test*",
			"test-config.groovy",
			"test/**",
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
This plugin, like reloadable config (http://www.grails.org/plugin/reloadable-config), has no dependencies but uses
simple Java timers.  However, unlike reloadable config, it is able to be fired at will and plugins can be notified
of configuration changes.

Please note: No warranty is implied or given with this plugin.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/plugin/external-config-reload"

    def doWithWebDescriptor = { xml ->
    }

    def doWithSpring = {
		def reloadConf = ReloadConfigUtility.loadConfig(application)
		configureServiceBean.delegate = delegate
		configureServiceBean(reloadConf, application)
    }

    def doWithDynamicMethods = { ctx ->
    }

    def doWithApplicationContext = { applicationContext ->
		def reloadConf = ReloadConfigUtility.loadConfig(application)
		def watchedFiles = getWatchedFiles(reloadConf, application)
		def reloadConfigService = applicationContext.getBean('reloadConfigService')
		reloadConfigService.plugins = reloadConf.notifyPlugins
		reloadConfigService.files = watchedFiles
		reloadConfigService.lastTimeChecked = new Date()
		reloadConfigService.automerge = reloadConf.automerge
		ReloadConfigUtility.configureWatcher(reloadConf, application)
    }

    def onChange = { event ->
		if (!event.source)
			return
		// Reload config service - inconsistent state due to multiple timers existing (old timer threads are
		//	not terminated)
		if (event.source.name=="grails.plugins.reloadconfig.ReloadConfigService") {
			def reloadConf = ReloadConfigUtility.loadConfig(event.application)
			def beans = beans {
				configureServiceBean.delegate = delegate
				configureServiceBean(reloadConf, event.application)
			}
			event.ctx.registerBeanDefinition("reloadConfigService", beans.getBeanDefinition("reloadConfigService"))
		}
		// Reload config watcher
		if (event.source.name=="grails.plugins.reloadconfig.ConfigWatcherJob") {
			def reloadConf = ReloadConfigUtility.loadConfig(event.application)
			ReloadConfigUtility.configureWatcher(reloadConf, event.application, true)
		}
    }

    def onConfigChange = { event ->
		// Reload config service and watcher job
		def reloadConf = ReloadConfigUtility.loadConfig(event.application)
		def watchedFiles = getWatchedFiles(reloadConf, event.application)
		def reloadConfigService = event.ctx.getBean('reloadConfigService')
		if (reloadConfigService?.timer?.cancelSchedule())
			log.info "Stopped configuration file watcher"
		reloadConfigService.plugins = reloadConf.notifyPlugins
		reloadConfigService.files = watchedFiles
		reloadConfigService.lastTimeChecked = new Date()
		reloadConfigService.automerge = reloadConf.automerge

		ReloadConfigUtility.configureWatcher(reloadConf, event.application, true)
    }

	private List getWatchedFiles(reloadConf, application) {
		def watchedFiles = reloadConf.files
		if (reloadConf.includeConfigLocations && application.config.grails.config.locations) {
			watchedFiles.addAll(application.config.grails.config.locations)
			watchedFiles = watchedFiles.unique()
		}
		return watchedFiles
	}
	
	def configureServiceBean = { reloadConf, application ->
		reloadConfigService(ReloadConfigService) {bean ->
            bean.autowire = "byName"
            bean.scope = "singleton"
        }
    }
}
