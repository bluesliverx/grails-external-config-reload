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
		compile 'net.sf.ezmorph:ezmorph:1.0.6', { excludes "commons-lang" }
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.

        // runtime 'mysql:mysql-connector-java:5.1.13'
		compile('org.tmatesoft.svnkit:svnkit:1.3.5') {
            excludes "jna", "trilead-ssh2", "sqljet"
			export = false
		}
    }
	plugins {		
		// Not exported
		test ':code-coverage:1.2.4', {
			export = false
		}
		test ':codenarc:0.15', {
			export = false
		}
		test ':spock:0.6', {
			export = false
		}
		build (':release:2.0.0.BUILD-SNAPSHOT') {
			export = false
			excludes "svn"
		}
		build(':svn:1.0.2') {
			export = false
			excludes "svnkit"
		}
	}
}
