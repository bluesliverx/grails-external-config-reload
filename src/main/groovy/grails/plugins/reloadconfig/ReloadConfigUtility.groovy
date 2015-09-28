package grails.plugins.reloadconfig

import grails.core.GrailsApplication
import grails.util.Environment
import groovy.util.logging.Slf4j

@Slf4j
class ReloadConfigUtility {

	private static String DEFAULT_CONFIG = """
		grails {
			plugins {
				reloadConfig {
					files = []
					includeConfigLocations = true
					interval = 5000
					enabled = true
					notifyPlugins = []
					automerge = true
					notifyWithConfig = true
				}
			}
		}
		environments {
			test {
				grails.plugins.reloadConfig.enabled = false
			}
		}"""

	// Borrowed from spring security
	public static ConfigObject loadConfig(GrailsApplication grailsApplication) {
		ConfigSlurper slurper = new ConfigSlurper(Environment.getCurrent().getName());
		ConfigObject config = (ConfigObject)slurper.parse(DEFAULT_CONFIG).grails.plugins.reloadConfig

		if (grailsApplication.config.grails.plugins.reloadConfig) {
			config.merge(grailsApplication.config.grails.plugins.reloadConfig as ConfigObject)
		}
		return config
	}
	
	public static void configureWatcher(ConfigObject reloadConf, grailsApplication, boolean restart=false) {
		def reloadConfigService = (ReloadConfigService)grailsApplication.mainContext.getBean("reloadConfigService")
		if (reloadConf.enabled) {
			def interval = reloadConf.interval
			if (!reloadConfigService.timer) {
				reloadConfigService.timer = new ReloadableTimer()
				reloadConfigService.timer.runnable = { reloadConfigService.checkNow() }
			}
			if (restart)
				log.info "Restarting configuration file watcher"
			else
				log.info "Starting configuration file watcher"
			if (reloadConfigService.timer.reschedule(interval))
				log.info "Stopped and restarted configuration file watcher"
		} else {
			log.info "Not watching configuration files"
			if (reloadConfigService.timer?.cancelSchedule())
				log.info "Stopped configuration file watcher"
		}
	}
}
