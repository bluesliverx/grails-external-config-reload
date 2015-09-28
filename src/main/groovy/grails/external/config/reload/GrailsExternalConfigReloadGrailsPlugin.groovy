package grails.external.config.reload

import grails.plugins.Plugin
import grails.plugins.reloadconfig.ReloadConfigService
import grails.plugins.reloadconfig.ReloadConfigUtility

class GrailsExternalConfigReloadGrailsPlugin extends Plugin {

    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "3.0.3 > *"

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
    def documentation = "http://grails.org/plugin/external-config-reload"

    def profiles = ['web']

    Closure doWithSpring() { {->
        def reloadConf = ReloadConfigUtility.loadConfig(grailsApplication)
        configureServiceBean.delegate = delegate
        configureServiceBean(reloadConf, grailsApplication)
        } 
    }

    void doWithDynamicMethods() {
    }

    void doWithApplicationContext() {
        def reloadConf = ReloadConfigUtility.loadConfig(grailsApplication)
        def watchedFiles = getWatchedFiles(reloadConf, grailsApplication)
        def reloadConfigService = applicationContext.getBean('reloadConfigService')
        reloadConfigService.plugins = reloadConf.notifyPlugins
        reloadConfigService.files = watchedFiles
        reloadConfigService.lastTimeChecked = new Date()
        reloadConfigService.automerge = reloadConf.automerge
        reloadConfigService.notifyWithConfig = reloadConf.notifyWithConfig

        reloadConfigService.checkNow(true)

        ReloadConfigUtility.configureWatcher(reloadConf, grailsApplication)
    }

    void onChange(Map<String, Object> event) {
        if (!event.source)
            return
        // Reload config service - inconsistent state due to multiple timers existing (old timer threads are
        //  not terminated)
        if (event.source.name=="grails.plugins.reloadconfig.ReloadConfigService") {
            def reloadConf = ReloadConfigUtility.loadConfig(event.grailsApplication)
            def beans = beans {
                configureServiceBean.delegate = delegate
                configureServiceBean(reloadConf, event.grailsApplication)
            }
            event.ctx.registerBeanDefinition("reloadConfigService", beans.getBeanDefinition("reloadConfigService"))
        }
        // Reload config watcher
        if (event.source.name=="grails.plugins.reloadconfig.ConfigWatcherJob") {
            def reloadConf = ReloadConfigUtility.loadConfig(event.grailsApplication)
            ReloadConfigUtility.configureWatcher(reloadConf, event.grailsApplication, true)
        }
    }

    void onConfigChange(Map<String, Object> event) {
        // Reload config service and watcher job
        def reloadConf = ReloadConfigUtility.loadConfig(event.grailsApplication)
        def watchedFiles = getWatchedFiles(reloadConf, event.grailsApplication)
        def reloadConfigService = event.ctx.getBean('reloadConfigService')
        if (reloadConfigService?.timer?.cancelSchedule())
            log.info "Stopped configuration file watcher"
        reloadConfigService.plugins = reloadConf.notifyPlugins
        reloadConfigService.files = watchedFiles
        reloadConfigService.lastTimeChecked = new Date()
        reloadConfigService.automerge = reloadConf.automerge
        reloadConfigService.notifyWithConfig = reloadConf.notifyWithConfig
        ReloadConfigUtility.configureWatcher(reloadConf, event.grailsApplication, true)
    }

    void onShutdown(Map<String, Object> event) {
    }

    private List getWatchedFiles(reloadConf, grailsApplication) {
        def watchedFiles = reloadConf.files
        if (reloadConf.includeConfigLocations && grailsApplication.config.grails.config.locations) {
            watchedFiles.addAll(grailsApplication.config.grails.config.locations.findAll { !(it instanceof Class) })
            watchedFiles = watchedFiles.unique()
        }
        return watchedFiles
    }

    def configureServiceBean = { reloadConf, grailsApplication ->
        reloadConfigService(ReloadConfigService) {bean ->
            bean.autowire = "byName"
            bean.scope = "singleton"
        }
    }

}
