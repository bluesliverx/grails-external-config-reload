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
	
	public static void configureWatcher(ConfigObject reloadConf, application, boolean restart=false, context=null) {
		if (context) {
			// Remove schedule if already scheduled
			def configWatcherClass = application.getTaskClass("grails.plugins.reloadconfig.ConfigWatcherJob")
			def quartzScheduler = context.getBean("quartzScheduler")
			quartzScheduler.getTriggersOfJob(configWatcherClass.fullName, configWatcherClass.group)?.each { trigger ->
				if (ConfigWatcherJob.unschedule(trigger.name))
					log.info "Stopped configuration file watcher"
			}
			
			// Set the initial run to prevent cyclic loading
			ConfigWatcherJob.initialRun = true
		}
		if (reloadConf.enabled) {
			if (restart)
				log.info "Restarting configuration file watcher"
			else
				log.info "Starting configuration file watcher"
			def files = reloadConf.files
			if (reloadConf.includeConfigLocations && application.config.grails.config.locations)
				files.addAll(application.config.grails.config.locations)
			def interval = reloadConf.interval
			ConfigWatcherJob.schedule(interval, -1, [
				interval:interval,
				files:files
			])
		} else {
			log.info "Not watching configuration files"
		}
	}
}
