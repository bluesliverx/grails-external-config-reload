package grails.plugins.reloadconfig

import grails.plugin.spock.*
import groovy.util.ConfigObject;

import org.codehaus.groovy.grails.commons.*
import org.springframework.context.ApplicationContext

class ReloadConfigUtilitySpec extends UnitSpec {
    def setup() {
		mockLogging(ReloadConfigUtility)
		registerMetaClass(ReloadConfigUtility)
    }

    def "Configure watcher disabled polling"() {
		given:
		DefaultGrailsApplication grailsApplication = Mock()
		ApplicationContext applicationContext = Mock()
		ReloadConfigService reloadConfigService = Mock()
		ReloadableTimer timer = Mock()
		
		and:
		def config = new ConfigObject()
		config.putAll([enabled:false])
		
		when:
		ReloadConfigUtility.configureWatcher(config, grailsApplication)
		
		then:
		1 * grailsApplication.getProperty('mainContext') >> applicationContext
		1 * applicationContext.getBean('reloadConfigService') >> reloadConfigService
		1 * reloadConfigService.getTimer()
		
		when:
		ReloadConfigUtility.configureWatcher(config, grailsApplication)
		
		then:
		1 * grailsApplication.getProperty('mainContext') >> applicationContext
		1 * applicationContext.getBean('reloadConfigService') >> reloadConfigService
		1 * reloadConfigService.getTimer() >> timer
		1 * timer.cancelSchedule() >> ret
		
		where:
		ret << [true, false]
    }
    def "Configure watcher enabled polling"() {
		given:
		DefaultGrailsApplication grailsApplication = Mock()
		ApplicationContext applicationContext = Mock()
		ReloadConfigService reloadConfigService = Mock()
		ReloadableTimer timer = Mock()
		
		and:
		def config = new ConfigObject()
		config.putAll([enabled:true, files:[], interval:1000, 
			includeConfigLocations:true, notifyPlugins:["test-plugin"]])
		
		when:
		ReloadConfigUtility.configureWatcher(config, grailsApplication)
		
		then:
		1 * grailsApplication.getProperty('mainContext') >> applicationContext
		1 * applicationContext.getBean('reloadConfigService') >> reloadConfigService
		2 * reloadConfigService.getTimer() >> timer
		1 * timer.reschedule(1000) >> rescheduleSucceed
		
		when:
		ReloadConfigUtility.configureWatcher(config, grailsApplication, true)
		
		then:
		1 * grailsApplication.getProperty('mainContext') >> applicationContext
		1 * applicationContext.getBean('reloadConfigService') >> reloadConfigService
		3 * reloadConfigService.getTimer() >>> [null, timer, timer]
		1 * reloadConfigService.setProperty("timer", _ as ReloadableTimer)
		1 * timer.setProperty("runnable", _ as Closure)
		1 * timer.reschedule(1000) >> rescheduleSucceed
		0 * _._
		
		where:
		rescheduleSucceed << [true, false]
    }
}
