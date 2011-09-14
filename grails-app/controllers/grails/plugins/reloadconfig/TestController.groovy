package grails.plugins.reloadconfig

class TestController {
	def reloadConfigService
	
    def index = {
		redirect(action:"checkNow")
	}
	
	def checkNow = {
		reloadConfigService.checkNow()
	}
	
	def reloadNow = {
		reloadConfigService.reloadNow()
	}
}
