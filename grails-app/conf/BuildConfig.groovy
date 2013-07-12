grails.work.dir = "target/workdir"
grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"
//grails.project.war.file = "target/${appName}-${appVersion}.war"

grails.release.scm.enabled = false

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

		test "org.spockframework:spock-grails-support:0.7-groovy-2.0", {
			export = false
		}
		build ("org.codehaus.groovy.modules.http-builder:http-builder:0.5.0") {
			excludes "commons-logging", "xml-apis", "groovy"
			export = false
		}
    }
	plugins {		
		// Not exported
		test ':code-coverage:1.2.6', {
			export = false
		}
		test ':codenarc:0.18.1', {
			export = false
		}
		test(":spock:0.7") {
			export = false
			exclude "spock-grails-support"
		}
		build (':release:2.2.1') {
			export = false
			excludes 'rest-client-builder'
		}
		build (':rest-client-builder:1.0.3') {
			export = false
		}
	}
}
