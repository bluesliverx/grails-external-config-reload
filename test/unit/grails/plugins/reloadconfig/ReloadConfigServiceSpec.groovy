package grails.plugins.reloadconfig

import grails.test.mixin.*
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.plugins.DefaultGrailsPluginManager
import spock.lang.*
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPlugin

/**
 * @author bsaville
 */
@TestFor(ReloadConfigService)
class ReloadConfigServiceSpec extends Specification {
	def setup() {
		grailsApplication.config.remove("key1")
		service.automerge = true
		service.grailsApplication = grailsApplication
	}

    def "Notify plugins"() {
		given:
		GrailsPluginManager pluginManager = Mock()
		service.pluginManager = pluginManager
		GrailsPlugin plugin = Mock()
		service.plugins = []
		service.grailsApplication = [config:[test:true]]

		when: "Notify using changed file list"
		service.notifyPlugins(["file1", "file2"])
		service.notifyPlugins(null)

		then:
		2 * pluginManager.getGrailsPlugin("external-config-reload") >> plugin
		1 * plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE, ["file1", "file2"])
		1 * plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE, null)
		0 * _._

		when: "Notify using configuration"
		service.notifyWithConfig = true
		service.notifyPlugins(["file1", "file2"])
		service.notifyPlugins(null)

		then:
		2 * pluginManager.getGrailsPlugin("external-config-reload") >> plugin
		2 * plugin.notifyOfEvent(GrailsPlugin.EVENT_ON_CONFIG_CHANGE, [test:true])
		0 * _._
    }

	def "Set plugins"() {
		expect:
		service.plugins==null

		when:
		service.plugins = plugins

		then:
		service.plugins
		service.plugins==result

		cleanup:
		service.@plugins = null

		where:
		plugins					|| result
		null					|| ["external-config-reload"]
		[]						|| ["external-config-reload"]
		["extra"]				|| ["extra", "external-config-reload"]
	}

	def "Notify plugins that do not exist"() {
		given:
		GrailsPluginManager pluginManager = Mock()
		service.pluginManager = pluginManager
		service.plugins = []

		when:
		service.notifyPlugins()

		then:
		1 * pluginManager.getGrailsPlugin("external-config-reload") >> null
		0 * _._
		notThrown(Exception)
	}

	def "Check now"() {
		given:
		def curDate = new Date()

		and:
		File.metaClass.exists = { ->
			return delegate.name!="notExists"
		}
		File.metaClass.lastModified = { ->
			if (delegate.name=="existsChanged")
				return curDate.time+1
			return (curDate - 1).time-1
		}
		File.metaClass.getText = { ->
			return "key1 = 'val1'"
		}

		and:
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { List changedFiles=null ->
			assert changedFiles.size()==1
			assert changedFiles[0].name=="existsChanged"
			notifyPluginsCalled = true
		}

		and:
		service.files = ["file:existsNotChanged", "notExists", "file:existsChanged"]
		service.lastTimeChecked = curDate - 1

		expect:
		service.lastTimeReloaded==null

		when:
		service.checkNow()

		then:
		notifyPluginsCalled
		service.lastTimeReloaded >= curDate
		service.lastTimeChecked >= curDate
		grailsApplication.config.key1=="val1"
	}

	def "Check now no automerge"() {
		given:
		service.automerge = false

		and:
		def curDate = new Date()
		File.metaClass.exists = { ->
			return true
		}
		File.metaClass.lastModified = { ->
			return curDate.time+1
		}

		and:
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { List changedFiles=null ->
			assert changedFiles.size()==1
			assert changedFiles[0].name=="existsChanged"
			notifyPluginsCalled = true
		}

		and:
		service.files = ["file:existsChanged"]
		service.lastTimeChecked = curDate - 1

		expect:
		service.lastTimeReloaded==null

		when:
		service.checkNow()

		then:
		notifyPluginsCalled
		service.lastTimeReloaded >= curDate
		service.lastTimeChecked >= curDate
		!grailsApplication.config.containsKey("key1")
	}

	def "Check now no files"() {
		given:
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}

		expect:
		service.lastTimeChecked==null
		service.lastTimeReloaded==null

		when:
		service.checkNow()

		then:
		!service.lastTimeReloaded
		service.lastTimeChecked
		!notifyPluginsCalled
	}

	def "Reload now"() {
		given:
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

		and:
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}

		and:
		def curDate = new Date()

		and:
		service.files = ["file:existsNotChanged", "notExists", "file:existsChanged"]
		service.lastTimeChecked = curDate - 1

		expect:
		service.lastTimeReloaded==null

		when:
		service.reloadNow()

		then:
		notifyPluginsCalled
		service.lastTimeReloaded >= curDate
		service.lastTimeChecked >= curDate
		grailsApplication.config.key1=="val1"
	}

	def "Reload now no automerge"() {
		given:
		service.automerge = false

		and:
		File.metaClass.exists = { ->
			return true
		}

		and:
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}

		and:
		def curDate = new Date()

		and:
		service.files = ["file:existsChanged"]
		service.lastTimeChecked = curDate - 1

		expect:
		service.lastTimeReloaded==null

		when:
		service.reloadNow()

		then:
		notifyPluginsCalled
		service.lastTimeReloaded >= curDate
		service.lastTimeChecked >= curDate
		!grailsApplication.config.containsKey("key1")
	}

	def "Reload now no files"() {
		given:
		def notifyPluginsCalled = false
		ReloadConfigService.metaClass.notifyPlugins = { ->
			notifyPluginsCalled = true
		}

		expect:
		service.lastTimeChecked==null
		service.lastTimeReloaded==null

		when:
		service.reloadNow()

		then:
		service.lastTimeChecked
		service.lastTimeReloaded
		notifyPluginsCalled
	}
}
