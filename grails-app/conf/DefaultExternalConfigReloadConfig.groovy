grails {
	plugins {
		reloadConfig {
			files = []
			includeConfigLocations = true
			interval = 5000
			enabled = true
			notifyPlugins = []
		}
	}
}
environments {
	test {
		grails.plugins.reloadConfig.enabled = false
	}
}