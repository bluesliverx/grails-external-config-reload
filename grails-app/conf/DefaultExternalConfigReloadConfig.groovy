grails {
	plugins {
		reloadConfig {
			files = []
			includeConfigLocations = true
			interval = 5000
			enabled = true
			notifyPlugins = []
			automerge = true
		}
	}
}
environments {
	test {
		grails.plugins.reloadConfig.enabled = false
	}
}