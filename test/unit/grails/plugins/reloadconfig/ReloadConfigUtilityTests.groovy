package grails.plugins.reloadconfig

import grails.test.*
import groovy.util.ConfigObject;
import org.codehaus.groovy.grails.commons.*
import org.quartz.*
import org.codehaus.groovy.grails.plugins.quartz.*

class ReloadConfigUtilityTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
		mockLogging(ReloadConfigUtility)
		registerMetaClass(ReloadConfigUtility)
		registerMetaClass(ConfigWatcherJob)
    }

    protected void tearDown() {
        super.tearDown()
    }

    void testConfigureWatcherDisabled() {
		def appMock = mockFor(GrailsApplication)
		def app = appMock.createMock()
		
		def config = new ConfigObject()
		config.putAll([enabled:false])
		boolean watcherScheduled = false
		ConfigWatcherJob.metaClass.'static'.schedule = { long interval, int repeatCount, Map params ->
			watcherScheduled = true
		}
		
		ReloadConfigUtility.configureWatcher(config, app)
		appMock.verify()
		assertFalse watcherScheduled
    }

    void testConfigureWatcherNoFilesIncludeConfigLocations() {
		def appMock = mockFor(GrailsApplication)
		appMock.demand.getConfig(2..2) { ->
			def config = new ConfigObject()
			config.putAll([grails:[config:[locations:["file:./test.groovy"]]]])
			return config
		}
		def app = appMock.createMock()
		
		def config = new ConfigObject()
		config.putAll([enabled:true, files:[], interval:1000, includeConfigLocations:true, notifyPlugins:["test-plugin"]])
		
		boolean watcherScheduled = false
		ConfigWatcherJob.metaClass.'static'.schedule = { long interval, int repeatCount, Map params ->
			assertEquals 1000, interval
			assertEquals(-1, repeatCount)
			assertEquals 2, params.size()
			assertEquals 1000, params.interval
			assertEquals 1, params.files.size()
			assertEquals "file:./test.groovy", params.files[0]
			watcherScheduled = true
		}
		
		ReloadConfigUtility.configureWatcher(config, app)
		appMock.verify()
		assertTrue watcherScheduled
    }

    void testConfigureWatcherFilesNoIncludeConfigLocations() {
		def appMock = mockFor(GrailsApplication)
		def app = appMock.createMock()
		
		def config = new ConfigObject()
		config.putAll([enabled:true, files:["file:./file.groovy"], interval:1000, includeConfigLocations:false, notifyPlugins:["test-plugin"]])
		
		boolean watcherScheduled = false
		ConfigWatcherJob.metaClass.'static'.schedule = { long interval, int repeatCount, Map params ->
			assertEquals 1000, interval
			assertEquals(-1, repeatCount)
			assertEquals 2, params.size()
			assertEquals 1000, params.interval
			assertEquals 1, params.files.size()
			assertEquals "file:./file.groovy", params.files[0]
			watcherScheduled = true
		}
		
		ReloadConfigUtility.configureWatcher(config, app)
		appMock.verify()
		assertTrue watcherScheduled
    }

    void testConfigureWatcherNoFilePrefixRestart() {
		def appMock = mockFor(GrailsApplication)
		def app = appMock.createMock()
		
		def config = new ConfigObject()
		config.putAll([enabled:true, files:["./file.groovy"], interval:1000, includeConfigLocations:false, notifyPlugins:["test-plugin"]])
		
		boolean watcherScheduled = false
		ConfigWatcherJob.metaClass.'static'.schedule = { long interval, int repeatCount, Map params ->
			assertEquals 1000, interval
			assertEquals(-1, repeatCount)
			assertEquals 2, params.size()
			assertEquals 1000, params.interval
			assertEquals 1, params.files.size()
			assertEquals "./file.groovy", params.files[0]
			watcherScheduled = true
		}
		
		ReloadConfigUtility.configureWatcher(config, app, true)
		appMock.verify()
		assertTrue watcherScheduled
    }

    void testConfigureWatcherWithContextNoTriggers() {		
		def appMock = mockFor(GrailsApplication)
		appMock.demand.getTaskClass { String className ->
			assertEquals "grails.plugins.reloadconfig.ConfigWatcherJob", className
			return new DefaultGrailsTaskClass(ConfigWatcherJob)
		}
		def app = appMock.createMock()
		
		
		def config = new ConfigObject()
		config.putAll([enabled:true, files:["./file.groovy"], interval:1000, includeConfigLocations:false, notifyPlugins:["test-plugin"]])
		
		boolean watcherScheduled = false
		boolean watcherUnscheduled = false
		ConfigWatcherJob.metaClass.'static'.schedule = { long interval, int repeatCount, Map params ->
			assertEquals 1000, interval
			assertEquals(-1, repeatCount)
			assertEquals 2, params.size()
			assertEquals 1000, params.interval
			assertEquals 1, params.files.size()
			assertEquals "./file.groovy", params.files[0]
			watcherScheduled = true
		}
		ConfigWatcherJob.metaClass.'static'.unschedule = { String name ->
			watcherUnscheduled = true
		}
		
		def quartzMock = mockFor(Scheduler)
		quartzMock.demand.getTriggersOfJob { String fullName, String group ->
			assertEquals "grails.plugins.reloadconfig.ConfigWatcherJob", fullName
			assertEquals "GRAILS_JOBS", group
			return null
		}
		applicationContext.registerMockBean("quartzScheduler", quartzMock.createMock())
		
		ReloadConfigUtility.configureWatcher(config, app, true, applicationContext)
		appMock.verify()
		assertTrue watcherScheduled
		assertFalse watcherUnscheduled
		quartzMock.verify()
    }

    void testConfigureWatcherWithContext() {		
		def appMock = mockFor(GrailsApplication)
		appMock.demand.getTaskClass { String className ->
			assertEquals "grails.plugins.reloadconfig.ConfigWatcherJob", className
			return new DefaultGrailsTaskClass(ConfigWatcherJob)
		}
		def app = appMock.createMock()
		
		
		def config = new ConfigObject()
		config.putAll([enabled:true, files:["./file.groovy"], interval:1000, includeConfigLocations:false, notifyPlugins:["test-plugin"]])
		
		boolean watcherScheduled = false
		ConfigWatcherJob.metaClass.'static'.schedule = { long interval, int repeatCount, Map params ->
			assertEquals 1000, interval
			assertEquals(-1, repeatCount)
			assertEquals 2, params.size()
			assertEquals 1000, params.interval
			assertEquals 1, params.files.size()
			assertEquals "./file.groovy", params.files[0]
			watcherScheduled = true
		}
		
		def triggerMock = mockFor(SimpleTrigger)
		triggerMock.demand.getName { ->
			return "triggerName"
		}
		
		def quartzMock = mockFor(Scheduler)
		quartzMock.demand.getTriggersOfJob { String fullName, String group ->
			assertEquals "grails.plugins.reloadconfig.ConfigWatcherJob", fullName
			assertEquals "GRAILS_JOBS", group
			return [triggerMock.createMock()] as Trigger[]
		}
		quartzMock.demand.unscheduleJob { String triggerName, String groupName ->
			assertEquals "triggerName", triggerName
			assertEquals "GRAILS_TRIGGERS", groupName
			return true	
		}
		applicationContext.registerMockBean("quartzScheduler", quartzMock.createMock())
		
		ReloadConfigUtility.configureWatcher(config, app, true, applicationContext)
		appMock.verify()
		assertTrue watcherScheduled
		quartzMock.verify()
		triggerMock.verify()
    }

    void testConfigureWatcherWithContextUnscheduleFail() {		
		def appMock = mockFor(GrailsApplication)
		appMock.demand.getTaskClass { String className ->
			assertEquals "grails.plugins.reloadconfig.ConfigWatcherJob", className
			return new DefaultGrailsTaskClass(ConfigWatcherJob)
		}
		def app = appMock.createMock()
		
		
		def config = new ConfigObject()
		config.putAll([enabled:true, files:["./file.groovy"], interval:1000, includeConfigLocations:false, notifyPlugins:["test-plugin"]])
		
		boolean watcherScheduled = false
		ConfigWatcherJob.metaClass.'static'.schedule = { long interval, int repeatCount, Map params ->
			assertEquals 1000, interval
			assertEquals(-1, repeatCount)
			assertEquals 2, params.size()
			assertEquals 1000, params.interval
			assertEquals 1, params.files.size()
			assertEquals "./file.groovy", params.files[0]
			watcherScheduled = true
		}
		
		def triggerMock = mockFor(SimpleTrigger)
		triggerMock.demand.getName { ->
			return "triggerName"
		}
		
		def quartzMock = mockFor(Scheduler)
		quartzMock.demand.getTriggersOfJob { String fullName, String group ->
			assertEquals "grails.plugins.reloadconfig.ConfigWatcherJob", fullName
			assertEquals "GRAILS_JOBS", group
			return [triggerMock.createMock()] as Trigger[]
		}
		quartzMock.demand.unscheduleJob { String triggerName, String groupName ->
			assertEquals "triggerName", triggerName
			assertEquals "GRAILS_TRIGGERS", groupName
			return false	
		}
		applicationContext.registerMockBean("quartzScheduler", quartzMock.createMock())
		
		ReloadConfigUtility.configureWatcher(config, app, true, applicationContext)
		appMock.verify()
		assertTrue watcherScheduled
		quartzMock.verify()
		triggerMock.verify()
    }
}
