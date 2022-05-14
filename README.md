# Miscellaneous PT CI/CD plugins bundle
Set of CI/CD plugins that allow to integrate Positive Technologies tools in build pipelines.
## Build plugins
Starting with plugins version 3.6.2 Gradle build script use com.palantir.git-version plugin to include SCM commit hash to manifests. That means you need use ```git clone``` command to download sources.  
### Build plugins using Gradle
To build plugins bundle using Gradle you need to execute ```build``` Gradle task:
```
$ ./gradlew build
```
Jenkins plugin will be built for CI version defined in ```gradle.properties``` file but may be redefined using ```-P``` option:
```
$ ./gradlew build -P jenkinsVersion=2.150.2
```
### Build plugins using Docker Gradle image
Execute ```docker run``` command in project root:
```
docker run --rm -u root -v "$PWD":/home/gradle/project -w /home/gradle/project gradle:6.8.3-jdk8 gradle build --no-daemon
```
## Jenkins plugin debugging
Jenkins Gradle plugin support starting CI server in debug mode that allows plugin developer to connect to server using IDE tools and debug plugin code. 
### Jenkins plugin debugging 
To start Jenkins with debug port 8000, execute ```server``` Gradle task with `--debug-jvm` flag:
```
$ ./gradlew server --debug-jvm
```
See additional info on gradle-jpi-plugin [page](https://github.com/jenkinsci/gradle-jpi-plugin).
