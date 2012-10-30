package grails.plugins.reloadconfig

import grails.util.Environment
import groovy.util.ConfigObject;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.apache.log4j.Logger

class ReloadConfigUtility {
	private static Logger log = Logger.getLogger(this)
	
	// Borrowed from spring security
	public static ConfigObject loadConfig(GrailsApplication application) {
		GroovyClassLoader classLoader = new GroovyClassLoader(ReloadConfigUtility.class.getClassLoader());
		ConfigSlurper slurper = new ConfigSlurper(Environment.getCurrent().getName());
		ConfigObject secondary = null;
		try {
			secondary = (ConfigObject)slurper.parse(classLoader.loadClass('DefaultExternalConfigReloadConfig')).grails.plugins.reloadConfig;
		} catch (Exception e) {
			// Fix this?
		}
		
		ConfigObject config = new ConfigObject();
		if (secondary == null) {
			config.putAll(application.config.grails.plugins.reloadConfig);
		} else {
			config.putAll(secondary.merge(application.config.grails.plugins.reloadConfig));
		}
		return config;
	}
	
	public static void configureWatcher(ConfigObject reloadConf, application, boolean restart=false) {
		def reloadConfigService = (ReloadConfigService)application.mainContext.getBean("reloadConfigService")
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
