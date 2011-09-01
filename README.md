This plugin reloads external configuration files (those files added to grails.config.locations or additional arbitrary ones) based on a Quartz job and then notifies any plugin specified of updates to the config files by firing the onConfigChange event.

## Configuration

There are several options available for configuration that can be placed in Config.groovy.  These options and their default values are shown below:

```groovy
grails.plugins.reloadConfig.files = []
grails.plugins.reloadConfig.includeConfigLocations = true
grails.plugins.reloadConfig.interval = 5000
grails.plugins.reloadConfig.enabled = true
grails.plugins.reloadConfig.notifyPlugins = []
```

Each of these options are described below.

### Files

The files option adds additional files that should be watched.  These should *not* include the files in grails.config.locations.  The default is an empty list.

### Include config.locations

This option specified whether the files in grails.config.locations are checked for modifications.  The default is true.

### Interval

This is the time in milliseconds that the polling job will run to check for file modifications.  The default is 5000, or 5 seconds.

### Enabled

If set to false, this will disable the polling job completely.  This may be used to disable polling in certain environments.  This is false by default in the test environment but true in all others.

### Notify Plugins

This option is a list of plugin names (as in \["external-config-reload"\]) that should be notified when a configuration file has been modified.  This will fire the onConfigChange event for each plugin individually in the order that they are specified.  The default is not to notify any plugins.


## Service

There is a ReloadConfigService that can be used to notify plugins of configuration changes on demand.  To use it, simply declare it and call notifyPlugins on it:

```groovy
class SomeService {
	def reloadConfigService
	
	def serviceMethod() {
		// Some logic here...
		reloadConfigService.notifyPlugins()
	}
}
```

This works exactly the same as if the configuration were changed, but no new configuration is loaded with this method.  The onConfigChange event for each plugin specified in the configuration is called.