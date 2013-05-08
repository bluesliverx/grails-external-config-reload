package grails.plugins.reloadconfig

import grails.test.mixin.*
import spock.lang.*
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPlugin

/**
 * @author bsaville
 */
@TestFor(ReloadConfigService)
class ReloadConfigServiceSpec extends Specification {
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
}
