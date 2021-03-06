[![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)](https://opensource.org/licenses/MIT) [![Javadocs](https://www.javadoc.io/badge/uk.cogfin/jfconfig.svg)](https://www.javadoc.io/doc/uk.cogfin/jfconfig) [![Maven](https://maven-badges.herokuapp.com/maven-central/uk.cogfin/jfconfig/badge.svg)](https://maven-badges.herokuapp.com/maven-central/uk.cogfin/jfconfig)


# JFConfig: Just configure

A dropwizard configuration factory to enable inheritance and imports from yaml config files and some utility methods to make it easy to configure any JVM application.

**Create and validate configuration objects from YAML with inheritance and imports** - create configurations for different deployment environments

**Override configuration using environment variables, system properties and/or external configuration file** - enable/require sys admin / configuration management to set or override config

**100% java implementation** - for compatibility with your favourite JVM language 


## Javadoc
[Latest release](https://javadoc.io/doc/uk.cogfin/jfconfig)

## Standalone (1 line) configuration

###### MyApp.groovy
```groovy
MyAppCfg validatedConfig = JFConfig.fromClasspath(MyAppCfg, 'config/production.yml')
```

###### MyAppCfg.groovy
```groovy
class MyAppCfg {

    @NotBlank
    String applicationName
    
    @NotNull
    @Valid
    MyCredentials myCredentials
    
    @Valid
    MyCredentials otherOptionalCredentials
}
```

###### MyCredentials.groovy
```groovy
class MyCredentials {

    @NotBlank
    String username
    
    @NotBlank
    String password
}
```

###### config/production.yml
```yaml
applicationName: Demo Application
myCredentials:
  username: ${MYAPP_USERNAME:-demoUser}
  password: ${MYAPP_PASSWORD:-}
```

###### build.gradle
```gradle
dependencies {
    compile "uk.cogfin:jfconfig:${jfconfigVersion}"
    
    ....
}
```

## Dropwizard configuration 

###### MyDropwizardApp.groovy
```groovy
void initialize(Bootstrap<T> bootstrap) {
    super.initialize(bootstrap)
    bootstrap.setConfigurationFactoryFactory(new DWConfigFactoryFactory<T>())
    bootstrap.setConfigurationSourceProvider(JFConfig.createEnvVarSubstitutingClasspathSourceProvider())

    ...
}
```


The above is equivalent to:

```groovy
void initialize(Bootstrap<T> bootstrap) {
    super.initialize(bootstrap)
    bootstrap.setConfigurationFactoryFactory(new DWConfigFactoryFactory<T>('inherits', 'import', 'jf-conf'))
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            new ResourceConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor(false)
        )
    )

    ...
}
```

## Inheritance

A configuration (YAML) can inherit from another configuration. The configuration that is inherited can be overriden by the inheriting config

A config can only inherit from a single other config, but that config can inherit from another and there is no fixed limit to the size of the inheritance tree
 
Inherited config supports imports in the same way as the main config

When the inheritance key (default "inherits") is found in the config then it must be followed by a location that can be resolved by the ConfigurationSourceProvider with which the config was loaded. The key will be removed from the config and the inherited config will be loaded

##### example

###### myapp.yml
```yaml
applicationName: My Application
showStackTrace: false
minifyAssets: true
```

###### aws.yml
```yaml
inherits: myapp.yml
imageRepository:
  type: s3
  secret: ${S3_SECRET:-}
  endpoint: ${S3_ENDPOINT:-xyz.amazonaws.com}
```

###### test.yml
```yaml
inherits: aws.yml
showStackTrace: true
minifyAssets: false
imageRepository:
  accessKey: ${S3_ACCESS_KEY:-testUser}
  bucket: test-images
```

###### prod.yml
```yaml
inherits: aws.yml
imageRepository:
  accessKey: ${S3_ACCESS_KEY:-prodAwsUser}
  bucket: production-images
```

###### qa.yml
```yaml
inherits: aws.yml
imageRepository:
  accessKey: ${S3_ACCESS_KEY:-qaAwsUser}
  bucket: qa-images
```

###### dev.yml
```yaml
inherits: myapp.yml
showStackTrace: true
minifyAssets: false
imageRepository:
  type: filesystem
  directory: ${HOME}/image-repo
```

## Imports

#### Simple import

The imported config can be overriden by the config that imports it

When the import key (default "import") is found in the config then the string value will be a location that will be resolved by the ConfigurationSourceProvider with which the config was loaded. The key will be removed from the config and the imported config will be loaded

In the example below, loading config-with-import.yml will result in a database config object with debug set to true 

###### config-with-import.yml
```yaml
applicationName: My Application
import: config/test-database.yml
database:
  debug: true
showStackTrace: false
minifyAssets: true
```

###### config/test-database.yml
```yaml
database:
  user: ${DB_USER:-}
  password: ${DB_PASSWORD:-}
  driver: org.postgresql.Driver
  url: jdbc:postgresql://test-db/myapp
  debug: false
```

#### Advanced import (map)

Advanced options can be enabled by providing a map for the import value.

|Key Name|Type|Required|Default|Description|
|--------|----|--------|-------|-----------|
|location|String|Yes| |location of the config to import|
|optional|boolean|No|false|importing config with optional=true will not fail to load the configuration if the imported config cannot be read|
|object|String|No| |import a select object from the config. dot separated path. when not specified, the whole config will be imported|
|target|String|No| |select where to merge the imported config into the importing config. dot separated path. when not specified will merge at the root|
|processParentAndImports|boolean|No|true|Whether to honour the parent and import keys in the config being imported|

###### config-with-optional-import.yml
```yaml
applicationName: My Application
import:
  location: build-version.yml
  optional: true
```

### TODO - object/target example

#### Multiple imports

Multiple files can be imported by providing a sequence as the value for the import key

Simple string imports and the advanced map form can both be used

###### multiple-imports.yml
```yaml
applicationName: My Application
import:
  - required-config1.yml
  - required-config2.yml
  - location: optional-config.yml
    optional: true
  - location: required-config3.yml
    optional: false
```

## Environment variables

Most of the utility methods in JFConfig wrap the source providers with a substituting source provider which will use the apache StrSubstitutor to replace environment variables in the configuration

The substitution will replace the string `${ENABLE_DEBUG}` in the config with the value of environment variable `ENABLE_DEBUG` **if the environment variable is set!**

For the above reason, it is recommended to **always supply a default value**

Default values are provided using the bash `:-` syntax, eg `${ENABLE_DEBUG:-false}` 

When there is no sensible default or you require an environment variable to be supplied, add an empty default `${ENV_VAR:-}` which will be replaced with nothing (interpreted as null with the default objectMapper) - a default of empty string can be supplied with `${ENV_VAR:-""}`

In the following example, the configuration will fail validation if either USE_THE_THING or MYAPP_PASSWORD is not set (additionally MYAPP_PASSWORD cannot be an empty String)

###### AppCfg.groovy
```groovy
    @NotNull
    Boolean useTheThing
    
    @NotBlank
    String username

    @NotBlank
    String password    
```

###### config.yml
```yaml
useTheThing: ${USE_THE_THING:-}
username: ${MYAPP_USERNAME:-myapp}
password: ${MYAPP_PASSWORD:-}
```

### Disabling environment variable substitution
 
Disable environment variable substitution by using a method in JFConfig that takes a ConfigurationSourceProvider

###### Load config from classpath without environment variable substitution
```groovy
MyAppCfg validatedConfig = JFConfig.fromSourceProvider(
                                 new ResourceConfigurationSourceProvider(),
                                 MyAppCfg,
                                 'config/production.yml')
```

## External configuration file

Whilst many applications are now deployed into containers and configured using environment variables,
there is still a need to be able to supply local information in an external configuration file i.e. desktop applications.

The external configuration file provides a way to supply an **optional** location for override configuration that will be loaded directly
to allow the rest of the config to be loaded from the configuration source provider i.e. from the classpath.

If the file does not exist, it is ignored and configuration continues to load.

The external config does not support environment variable substitution.

## System property overrides

Configuration properties can be overridden with system properties. This happens after the configuration has been fully resolved (imports and inheritance) but before it is mapped onto the configuration object and validated.

System properties starting with the propertyOverridePrefix (default `jf-conf`) followed by a `.` and a path to a property will override that property.
 
See the **note at the bottom** of the [Configuration section in the Dropwizard manual](https://www.dropwizard.io/1.2.2/docs/manual/core.html#configuration) and substitute `dw.` with `jf-conf.` or whatever you have configured your prefix to

##### example

###### config.yml
```yaml
remoteService:
  username: appUser
  password: ${REMOTE_SERVICE_PASSWORD:-}
```

The remoteService username property can be overridden by using system properties

```sh
java -Djf-conf.remoteService.username=testUser -jar myApp.jar
```

Map keys that contain dots need to be escaped with a backslash. Depending on how/ where you set the system override, the backslash may also need to be escaped:

##### Set the log level for a logger in Dropwizard
```java
System.setProperty("jf-conf.logging.loggers.org\\.apache\\.shiro\\.mgt", "TRACE")
```

## Validation

Validation is provided by [Hibernate Validator](https://docs.jboss.org/hibernate/validator/5.4/reference/en-US/html_single/#section-declaring-bean-constraints)

## Polymophism

Polymorphic configuration is provided by Jackson and the dropwizard Discoverable marker interface

[Polymorphic configuration in the Dropwizard documentation](http://www.dropwizard.io/1.2.2/docs/manual/configuration.html#polymorphic-configuration)

[Jackson polymorphic deserialization](https://github.com/FasterXML/jackson-docs/wiki/JacksonPolymorphicDeserialization)

## YAML references (and Jackson impl)

### TODO

## Utilities

### Validating config

To validate a configuration without starting your application, just load it! Validation is always performed and an exception will be thrown containing details of failures if the config could not be loaded or was invalid

###### ValidateConfig.groovy
```groovy
    static void main(String[] args) {
        String configLocation = args[0]
        JFConfig.fromClasspath(MyApplicationConfig, configLocation)
    }
```

### Printing fully resolved config

A utility method is provided to serialize any object to YAML. Depending on object type and Jackson JSON annotations, this may not look like the source yaml you configured it with, but can be very useful for debugging
 
###### PrintValidatedConfig.groovy
```groovy
    static void main(String[] args) {
        String configLocation = args[0]
        MyApplicationConfig config = JFConfig.fromClasspath(MyApplicationConfig, configLocation)
        JFConfig.printConfig(System.out, config)
    }
```

### Print config tree before binding and validation with environment variable substitution

Print the fully resolved config tree before attempting to bind to any objects

**As system property overrides are applied in the dropwizard base class just prior to object binding, they will not be set before printing**

###### PrintRawConfigWithEnvVarReplacement.groovy
```groovy
    static void main(String[] args) {
        String configLocation = args[0]
        def sourceProvider = JFConfig.createEnvVarSubstitutingClasspathSourceProvider()
        JFConfig.printConfigTree(System.out, sourceProvider, configLocation)
    }
```

### Print config tree before binding and validation without env var substitution

Print the fully resolved config tree before attempting to bind to any objects without replacing environment variables

**As system property overrides are applied in the dropwizard base class just prior to object binding, they will not be set before printing**

###### PrintRawConfigWithEnvVarReplacement.groovy
```groovy
    static void main(String[] args) {
        String configLocation = args[0]
        ConfigurationSourceProvider sourceProvider = new ResourceConfigurationSourceProvider()
        JFConfig.printConfigTree(System.out, sourceProvider, configLocation)
    }
```


