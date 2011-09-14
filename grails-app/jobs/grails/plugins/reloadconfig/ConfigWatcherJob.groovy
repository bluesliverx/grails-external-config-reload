package grails.plugins.reloadconfig

class ConfigWatcherJob {
	static Boolean initialRun = true
	def reloadConfigService
	
    def execute(context) {
		// Prevents cyclic reloads when a reload is triggered
		if (initialRun) {
			initialRun = false
			return
		}
		
		reloadConfigService.checkNow()
    }
}
