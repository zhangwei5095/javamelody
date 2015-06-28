# Nightly build, compilation and development of Java Melody #

### Download nightly build ###

> You can download the [nightly build](https://javamelody.ci.cloudbees.com/job/javamelody/) (snapshot from trunk), from [CloudBees](http://www.cloudbees.com/) Jenkins CI. <a href='http://www.cloudbees.com/'><img src='resources/images/Button-Built-on-CB-1.png' alt='Built on CloudBees' width='20%' /></a>

### Browsing the sources and javadoc ###

> You can [browse](../master) the development sources from svn or you can [download](../releases) the zip file of a release which includes the sources of that release.

> You can also browse the latest [javadoc](https://javamelody.ci.cloudbees.com/job/javamelody/site/apidocs/index.html), [sources](https://javamelody.ci.cloudbees.com/job/javamelody/site/xref/index.html) and other [reports](https://javamelody.ci.cloudbees.com/job/javamelody/) from the [CloudBees](http://www.cloudbees.com/) Jenkins CI (snapshot from trunk).

> You can also browse the sources at [grepcode](http://grepcode.com/file/repo1.maven.org/maven2/net.bull.javamelody/javamelody-core/1.55.0/net/bull/javamelody/MonitoringFilter.java) with point and click, except that grepcode does not display the latest version in general.

### Building from sources ###

> The zip file available in "[Download](../releases)" contains the java sources with an [ant](http://ant.apache.org) script build.xml and a pom.xml file for [maven](http://maven.apache.org)
> and contains also the javadoc, the eclipse project, the jar files.
> If you want to rebuild the development version and not a particular release, clone from
> http://javamelody.googlecode.com/svn/trunk/javamelody-core/ [TODO]

> After decompressing the zip file or after the checkout, you can use the "all" target of the ant script (without maven) to recompile from sources
> and to build the javamelody.jar, jira-javamelody.jar and javamelody.war files which are those available in "Releases".
> You must define the environment variable JAVA\_HOME with the path
> of a JDK 1.7 to compile the sources.

> For example:

```
	<path_to_ant>/bin/ant -f <path_to_javamelody>/build.xml all
```

> You can also use the maven goals "clean site" to build a maven site with documentations and reports on analysis of sources
> or on unit tests (checkstyle, findbugs, pmd, cpd, junit, cobertura). You will need Maven 3.0.4 or later for that.


### Development ###

> The development is done with [Eclipse](http://www.eclipse.org) in its latest version and with the jdk 1.7 at least for compilation.

> The manager of sources is GitHub at	http://javamelody.googlecode.com/svn/trunk/ [TODO]

> The charset of files is UTF-8. The Eclipse configuration is supplied in sources with the configuration of
> code formater, clean-up, warnings and of checkstyle, pmd, findbugs and lint4j plugins.
> The infinitest eclipse plugin is also recommended.

> The official languages of the project are french for javadoc but english for technical terms.
> (This provides a clean separation between functional and technical vocabulary, given that you speak French obviously).

> The diagrams in the OpenOffice [map](../raw/wiki/resources/Map.odp) or the other [one](../raw/wiki/resources/MethodCallsMap.odp) may help to understand.

### Tests ###

> The JUnit tests can be launched with the "test" target of the ant script, or with maven or in [ContinuousIntegration](ContinuousIntegration.md).

> The manual tests are written in the OpenOffice [Test plan](../raw/wiki/resources/Test%20plan.ods).

### Plugins ###

> [JIRA/Confluence/Bamboo plugin](https://plugins.atlassian.com/plugins/net.bull.javamelody) : The sources for the JIRA/Confluence/Bamboo plugin are with the sources of JavaMelody at http://javamelody.googlecode.com/svn/trunk/javamelody-core/. As said above, you can use the "all" target of the ant script to build the jira-javamelody.jar file.

> [Jenkins plugin](http://wiki.jenkins-ci.org/display/JENKINS/Monitoring) : The manager of sources for the Jenkins Monitoring plugin is git at https://github.com/jenkinsci/monitoring-plugin (previously Subversion at http://svn.jenkins-ci.org/trunk/hudson/plugins/monitoring/). Use maven commands "mvn hpi:run" or "mvn package" like for all [Jenkins plugins](http://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial).

> [Grails plugin](http://www.grails.org/plugin/grails-melody) : The manager of sources for the JavaMelody Grails plugin is git at https://github.com/evernat/grails-melody-plugin (previously Subversion at http://svn.codehaus.org/grails-plugins/grails-grails-melody/trunk/)

> [Liferay plugin](UserGuide#Liferay_Plugin.md) : The manager of sources for the JavaMelody plugin for Liferay is git at https://github.com/evernat/liferay-javamelody

> [Sonar plugin](UserGuide#Sonar_Plugin.md) : The manager of sources for the JavaMelody plugin for Sonar is git at https://github.com/evernat/sonar-javamelody

> [Grails plugin](http://www.grails.org/plugin/grails-melody) : The manager of sources for the JavaMelody Grails plugin is git at https://github.com/evernat/grails-melody-plugin

### Other plugins ###

> [Gerrit server plugin](https://gerrit.googlesource.com/plugins/javamelody/) : There is also a Gerrit server plugin by David Ostrovsky. The manager of sources is Git at https://gerrit.googlesource.com/plugins/javamelody/+/master

> [Nagios plugin](https://github.com/sbower/nagios_javamelody_plugin) : There is also a Nagios plugin by Shawn Bower based on a command reading the last values in the RRD files (it starts a JVM for each value, which may not scale a lot). The manager of sources is Git at https://github.com/sbower/nagios_javamelody_plugin


### Release process ###

This is an internal reminder of the steps of the release process.

  * Edit Maven password
  * **javamelody-core** (https://javamelody.googlecode.com/svn/trunk/javamelody-core)
    * revert files if needed
    * mvn release:prepare release:perform
    * tag is created in http://javamelody.googlecode.com/svn/tags/
  * call **ant** target "zip" of build.xml to package files
  * Edit version in README.md at https://github.com/javamelody/javamelody/blob/master/README.md
  * **draft a new release** and **upload** jar, war, zip and jira-javamelody.jar files to https://github.com/javamelody/javamelody/releases
  * **update [Release notes](http://code.google.com/p/javamelody/wiki/ReleaseNotes)** with version
  * **increment versions in other pom.xml files**: parent, javamelody-collector-server, javamelody-test-webapp, javamelody-swing, javamelody-for-standalone
  * **publish artefacts in Maven central**
    * mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=javamelody-core-version.pom -Dfile=javamelody-core-version.jar
    * mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=javamelody-core-version.pom -Dfile=javamelody-core-version-sources.jar -Dclassifier=sources
    * mvn gpg:sign-and-deploy-file -Durl=https://oss.sonatype.org/service/local/staging/deploy/maven2/ -DrepositoryId=sonatype-nexus-staging -DpomFile=javamelody-core-version.pom -Dfile=javamelody-core-version-javadoc.jar -Dclassifier=javadoc
    * https://oss.sonatype.org/ : log in, close and release
  * **jenkins / hudson plugin** (https://github.com/jenkinsci/monitoring-plugin ~~https://svn.jenkins-ci.org/trunk/hudson/plugins/monitoring~~)
    * increment javamelody-core version in pom.xml
    * ~~mvn release:prepare release:perform~~
    * mvn versions:set -DnewVersion=1.xx.0 -DgenerateBackupPoms=false
    * mvn clean deploy
    * mvn versions:set -DnewVersion=1.xy.0-SNAPSHOT -DgenerateBackupPoms=false
    * edit release notes for version: https://wiki.jenkins-ci.org/display/JENKINS/Monitoring
  * **jira / confluence / bamboo plugin**
    * login https://plugins.atlassian.com/plugins/net.bull.javamelody
    * add version with jira-javamelody-version.jar
  * **sonar plugin**
    * increment javamelody-core version and project's version in https://github.com/evernat/sonar-javamelody/blob/master/pom.xml
    * mvn clean package to package the jar file in the target directory
    * create [release](https://github.com/evernat/sonar-javamelody/releases) and attach the jar file
  * **liferay plugin**
    * increment javamelody-core version and project's version in https://github.com/evernat/liferay-javamelody/blob/master/pom.xml
    * mvn clean package to package the jar file in the target directory
    * create [release](https://github.com/evernat/liferay-javamelody/releases) and attach the jar file
  * **wait artefacts** to be available in Maven central http://search.maven.org/#search|ga|1|javamelody
  * increment maven version in [UserGuide#Dependencies](UserGuide#Dependencies.md)
  * **grails plugin** (https://github.com/evernat/grails-melody-plugin, [grails documentation](http://grails-plugins.github.io/grails-release/docs/manual/guide/plugins.html))
    * increment javamelody-core version in grails-app/conf/BuildConfig.groovy
    * increment plugin version in GrailsMelodyGrailsPlugin.groovy
    * edit user / password in .grails\settings.groovy
    * grails publish-plugin
    * (Edit plugin if needed in http://www.grails.org/plugin/grails-melody)
  * **publish** drafted release at https://github.com/javamelody/javamelody/releases
  * **twitter announce** https://twitter.com/#!/java_melody
  * **users' group announce** https://groups.google.com/forum/?fromgroups#!forum/javamelody ([example](https://groups.google.com/forum/?fromgroups#!topic/javamelody/w-8Xhi7G5x8))
