package grails.plugins.reloadconfig

import org.junit.*
import static org.junit.Assert.*
import grails.test.mixin.*
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.commons.*

@TestFor(ReloadConfigService)
class ReloadConfigServiceTests {
	@Before
	void setup() {
		grailsApplication.config.remove("key1")
		grailsApplication.config.grails.plugins.reloadConfig.automerge = true
		service.grailsApplication = grailsApplication
	}

	void testSetPlugins() {
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
		DefaultGrailsPluginManager.metaClass.getGrailsPlugin = { String plugin ->
			assertEquals "external-config-reload", plugin
			return null
		}
		
		service.plugins = []
		service.pluginManager = new DefaultGrailsPluginManager("", new DefaultGrailsApplication())
		service.notifyPlugins();
    }
	
	void testCheckNow() {
		def curDate = new Date()
		
		File.metaClass.exists = { ->
			return delegate.name!="notExists"
		}
		File.metaClass.lastModified = { ->
			if (delegate.name=="existsChanged")
				return curDate.time+1
			return curDate.time-1
		}
		File.metaClass.getText = { ->
			return "key1 = 'val1'"
		}
		
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { List changedFiles=null ->
			assert changedFiles.size()==1
			assert changedFiles[0].name=="existsChanged"
			notifyPluginsCalled = true
		}
		
		service.files = ["file:existsNotChanged", "notExists", "file:existsChanged"]
		service.lastTimeChecked = curDate
		
		service.checkNow()
				
		assertTrue notifyPluginsCalled
		assertTrue service.lastTimeChecked > curDate
		assert grailsApplication.config.key1=="val1"
	}
	
	void testCheckNowNoAutomerge() {
		grailsApplication.config.grails.plugins.reloadConfig.automerge = false
		
		def curDate = new Date()
		File.metaClass.exists = { ->
			return true
		}
		File.metaClass.lastModified = { ->
			return curDate.time+1
		}
		
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { List changedFiles=null ->
			assert changedFiles.size()==1
			assert changedFiles[0].name=="existsChanged"
			notifyPluginsCalled = true
		}
		
		service.files = ["file:existsChanged"]
		service.lastTimeChecked = curDate
		
		service.checkNow()
				
		assertTrue notifyPluginsCalled
		assertTrue service.lastTimeChecked > curDate
		assert !grailsApplication.config.containsKey("key1")
	}
	
	void testCheckNowNoFiles() {
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}
		
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
		
		service.files = ["file:existsNotChanged", "notExists", "file:existsChanged"]
		service.lastTimeChecked = curDate
		
		service.reloadNow()
		
		assertTrue notifyPluginsCalled
		assertTrue service.lastTimeChecked > curDate
		assert grailsApplication.config.key1=="val1"
	}
	
	void testReloadNowNoAutomerge() {
		grailsApplication.config.grails.plugins.reloadConfig.automerge = false
		
		File.metaClass.exists = { ->
			return true
		}
		
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}
		
		def curDate = new Date()
		
		service.files = ["file:existsChanged"]
		service.lastTimeChecked = curDate
		
		service.reloadNow()
		
		assertTrue notifyPluginsCalled
		assertTrue service.lastTimeChecked > curDate
		assert !grailsApplication.config.containsKey("key1")
	}
	
	void testReloadNowNoFiles() {
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}
		
		assertNull service.lastTimeChecked
		service.reloadNow()
		
		assertNotNull service.lastTimeChecked
		assertTrue notifyPluginsCalled
	}
}
