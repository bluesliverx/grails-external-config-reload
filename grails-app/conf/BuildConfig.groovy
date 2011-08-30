grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"

// Code Narc
codenarc.reports = {
	XmlReport('xml') {
		outputFile = 'target/test-reports/CodeNarcReport.xml'
		title = 'External Config Reload Plugin Report'
	}
	HtmlReport('html') {
		outputFile = 'target/test-reports/CodeNarcReport.html'
		title = 'External Config Reload Plugin Report'
	}
}
codenarc.extraIncludeDirs = [
	'grails-app/jobs',
]

// Cobertura config (code coverage)
coverage {
	exclusions = [
		'**/com/burtbeckwith/**',
		'**/com/grailsrocks/**',
		'**/grails/plugins/**',
		'**/grails/plugin/**',
		'**/org/grails/tomcat/**',
		"**/*Test*",
		"**/test/**",
	]
	sourceInclusions = [
		'grails-app/jobs',
	]
}

grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsPlugins()
        grailsHome()
        grailsCentral()

        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenLocal()
        //mavenCentral()
        //mavenRepo "http://snapshots.repository.codehaus.org"
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.13'
    }
	plugins {
		compile ':quartz:0.4.2'
		runtime ':release:1.0.0.RC3' {
			export = false
		}
	}
}
