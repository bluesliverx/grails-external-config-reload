package grails.plugins.reloadconfig

import grails.test.*
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.commons.*

class ReloadConfigServiceTests extends GrailsUnitTestCase {
    protected void setUp() {
        super.setUp()
		mockLogging(ReloadConfigService)
		registerMetaClass(ReloadConfigService)
		registerMetaClass(DefaultGrailsPlugin)
		registerMetaClass(DefaultGrailsPluginManager)
		registerMetaClass(File)
    }

    protected void tearDown() {
        super.tearDown()
    }
	
	void testSetPlugins() {
		ReloadConfigService service = new ReloadConfigService()
		assertNull service.plugins
		
		service.plugins = null
		assert service.plugins
		assertEquals 1, service.plugins.size()
		assertEquals "external-config-reload", service.plugins[0]
		
		service.plugins = []
		assert service.plugins
		assertEquals 1, service.plugins.size()
		assertEquals "external-config-reload", service.plugins[0]
		
		service.plugins = ["some-other-plugin"]
		assert service.plugins
		assertEquals 2, service.plugins.size()
		assertEquals "some-other-plugin", service.plugins[0]
		assertEquals "external-config-reload", service.plugins[1]
	}

	// Not working due to mocking, removing for now
//    void testNotifyPlugins() {
//		ReloadConfigService service = new ReloadConfigService()
//		
//		def notifyCalled = false
//		def pluginMock = mockFor(GrailsPlugin)
//		pluginMock.demand.notifyOfEvent { int event, Object source ->
//			assertEquals event, GrailsPlugin.EVENT_ON_CONFIG_CHANGE
//			assertNull source
//			notifyCalled = true
//			return [:]
//		}
//		
//		DefaultGrailsPluginManager.metaClass.getGrailsPlugin = { String plugin ->
//			assertEquals "external-config-reload", plugin
//			return pluginMock.createMock()
//		}
//		
//		service.plugins = []
//		service.pluginManager = new DefaultGrailsPluginManager("", new DefaultGrailsApplication())
//		service.notifyPlugins();
//		
//		pluginMock.verify()
//    }

    void testNotifyPluginsDoesNotExist() {
		ReloadConfigService service = new ReloadConfigService()
		
		DefaultGrailsPluginManager.metaClass.getGrailsPlugin = { String plugin ->
			assertEquals "external-config-reload", plugin
			return null
		}
		
		service.plugins = []
		service.pluginManager = new DefaultGrailsPluginManager("", new DefaultGrailsApplication())
		service.notifyPlugins();
    }
	
	void testCheckNow() {
		def firstExists = true
		File.metaClass.exists = { ->
			if (firstExists) {
				firstExists = false
				return true
			}
			return false
		}
		def firstLastModified = true
		File.metaClass.lastModified = { ->
			if (firstLastModified) {
				firstLastModified = false
				return new Date().time
			}
			return 1
		}
		File.metaClass.getText = { ->
			return "key1 = 'val1'"
		}
		
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}
		
		
		def curDate = new Date()
		def service = new ReloadConfigService()
		service.grailsApplication = [config:[merge:{ ConfigObject config ->
			assertEquals 1, config.size()
			assertEquals "val1", config.key1
		}]]
		service.files = ["file:existsNotChanged", "notExists", "file:existsChanged"]
		service.lastTimeChecked = curDate
		
		service.checkNow()
				
		assertTrue notifyPluginsCalled
		assertTrue service.lastTimeChecked > curDate
	}
	
	void testCheckNowNoFiles() {
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}
		
		def service = new ReloadConfigService()
		assertNull service.lastTimeChecked
		service.checkNow()
		assertNotNull service.lastTimeChecked
		assertFalse notifyPluginsCalled
	}
	
	void testReloadNow() {
		def firstExists = true
		File.metaClass.exists = { ->
			if (firstExists) {
				firstExists = false
				return true
			}
			return false
		}
		File.metaClass.getText = { ->
			return "key1 = 'val1'"
		}
		
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}
		
		def curDate = new Date()
		def service = new ReloadConfigService()
		service.grailsApplication = [config:[merge:{ ConfigObject config ->
			assertEquals 1, config.size()
			assertEquals "val1", config.key1
		}]]
		service.files = ["file:existsNotChanged", "notExists", "file:existsChanged"]
		service.lastTimeChecked = curDate
		
		service.reloadNow()
		
		assertTrue notifyPluginsCalled
		assertTrue service.lastTimeChecked > curDate
	}
	
	void testReloadNowNoFiles() {
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}
		
		def service = new ReloadConfigService()
		assertNull service.lastTimeChecked
		service.reloadNow()
		
		assertNotNull service.lastTimeChecked
		assertTrue notifyPluginsCalled
	}
}
