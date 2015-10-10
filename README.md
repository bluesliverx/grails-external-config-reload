[![Build Status](https://travis-ci.org/bluesliverx/grails-external-config-reload.svg?branch=master)](https://travis-ci.org/bluesliverx/grails-external-config-reload)

This plugin reloads external configuration files (those files added to grails.config.locations or additional arbitrary ones)
based on a timer and then notifies any plugin specified of updates to the config files by firing the onConfigChange event.

**Note: Version 1.3.0 introduced a breaking change in how plugins are notified.  See the "Notify with Config" configuration parameter.**

## Configuration

There are several options available for configuration that can be placed in Config.groovy.  These options and their default values are shown below:

```groovy
grails.plugins.reloadConfig.files = []
grails.plugins.reloadConfig.includeConfigLocations = true
grails.plugins.reloadConfig.interval = 5000
grails.plugins.reloadConfig.enabled = true
grails.plugins.reloadConfig.notifyPlugins = []
grails.plugins.reloadConfig.automerge = true
grails.plugins.reloadConfig.notifyWithConfig = true
```

Each of these options are described below.

### Files

The files option adds additional files that should be watched.  These should *not* include the files in grails.config.locations.
The default is an empty list.

### Include config.locations

This option specified whether the files in grails.config.locations are checked for modifications.  The default is true.

### Interval

This is the time in milliseconds that the polling job will run to check for file modifications.  The default is 5000, or 5 seconds.

### Enabled

If set to false, this will disable the polling job completely.  This may be used to disable polling in certain environments.
This is false by default in the test environment but true in all others.

### Notify Plugins

This option is a list of plugin names (as in \["external-config-reload"\]) that should be notified when a configuration
file has been modified.  This will fire the onConfigChange event for each plugin individually in the order that they are
specified.  The default is not to notify any plugins.  The event passed into the onConfigChange closure will have the
"source" property loaded with all the changed configuration files detected or with the updated configuration (see
`notifyWithConfig`).

Note that the source may be null if this is called through typical Grails means (Config.groovy is
changed in development) in older versions of Grails or if something else calls the notifyPlugins method on the service
without any parameters.

### Automerge

This option, which defaults to true, can be set to false to not automatically load and merge the configuration files into
the current application configuration when a reload is triggered (either through a reloadNow or a polling check).
This is designed to allow a user to add custom handling of configuration files in the onConfigChange event in their plugin.
This also allows the user to parse and merge configuration which is not ConfigSlurper compatible groovy, such as XML files or others.

Note that if both this and notifyWithConfig are set to false, there is no current way to determine which files actually
changed.  For this reason, it is recommended to have notifyWithConfig set to false when this parameter is set to false.

### Notify with Config

This option is provided to support some plugins (such as the Grails logging plugin) which expect the configuration to be
sent as the parameter to the onConfigChange event.  If false, the plugin will notify other plugins with the
changed file list.  If true (default), it will notify other plugins with the updated configuration.  This is a breaking
change with version 1.3.0 of the plugin and should be set to false in applications that were depending on the changed
file list as the "source" property input.

It should be noted that the parameter being set to true mimics the behavior of Grails itself in more recent versions.

## Service

There is a ReloadConfigService that can be used to trigger config reload events on demand.  To use it, simply declare it and call the methods on it:

```groovy
class SomeService {
	def reloadConfigService
	
	def serviceMethod() {
		// Some logic here...
		
		reloadConfigService.checkNow()
		// OR...
		reloadConfigService.reloadNow()
		// OR...
		reloadConfigService.notifyPlugins()
		// OR...
		reloadConfigService.notifyPlugins([new File("my-config.groovy"), new File("other-file.properties")])

		// Know the last time the config files were reloaded (triggered by reloadNow or when changes occurred)
		reloadConfigService.getLastTimeReloaded()
	}
}
```

### Check Now
This triggers a check to be run on all watched files.  If a file has been modified, it will be merged into the current
configuration, and plugins will then be notified.

### Reload Now
This triggers a manual reload.  All configuration files that are being watched will be reloaded and merged into the current
configuration, and plugins will be notified.

### Notify Plugins
This works exactly the same as if the configuration were changed, but no new configuration is loaded with this method.
The onConfigChange event for each plugin specified in the configuration is called.  The optional list of files changed
can be passed in for more information in the onConfigChange event if "notifyWithConfig" is set to false.

Note that the "source" property of the event may be changed by the "notifyWithConfig" configuration parameter, regardless
of the list of changed files set as input to the method.


## Release Notes

### 2.0

* Grails 3 support (thanks to dtanner for upgrading the plugin!)

### 1.4.1

* Fixed #22 removed dependency on log4j and replaced with slfj4 (thanks to herom for the pull request!)

### 1.4.0

* Special thanks to erezool for fixing many of the issues with this release and for contributions!
* Fixed #14 spock dependency no longer leaks from the plugin
* (erezool) Fixed #15 no longer crash when the config file cannot be loaded for any reason
* Fixed #16 to add the "lastTimeReloaded" property to reload config service
* (erezool) Fixed #17 support for properties files instead of just groovy config files
* Fixed #19 decrease logging threshold to trace on many common logging statements

### 1.3.0

* Fixed #11 to add the "notifyWithConfig" configuration parameter (breaking change!)
* Fixed #12 to ignore grails.config.locations that are classes
* Fixed #13 to support grails 2.2.x

### 1.2.2

* Fixed #10 to not do multiple notifications for a single watched file.

### 1.2.1

* Fixed #9 to default the automerge option to true.

### 1.2.0

* Fixed #8 for adding the automerge configuration parameter.

### 1.1.0

* Added list of changed files as the event source when onConfigChange event is fired.

### 1.0.1

* Fixed #5 - Changed log levels so that info would catch reload events and changed configuration files

### 1.0.0

* Fixed #3 - Removed quartz dependency and used simple timers instead for scheduling the checkNow call

### 0.4.9

* Updated dependency declaration for xom (1.2 was apparently removed from some repositories)

### 0.4.8

* Small bug fix for warnings in the log file

### 0.4.7

* Fixed #2 for manual reloading

### 0.4.6

* Fixed #2 for periodic reloading

### 0.4.5

* Fixed #1
* Implemented reloadNow() and checkNow() on reload config service.
* Improved last update time checking to not use simply the job interval anymore

### 0.4.3

* Added service to notify plugins on demand (outside of the configuration changes)
* Updated docs to show how to set correct configuration options
* Verified compatibility with Grails 1.2 and 2.0.0.M1

#### 0.4.1

Initial public release