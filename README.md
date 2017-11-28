# JFConfig

#### Create and validate configuration objects from YAML with inheritance, imports and overrides by external configuration file, system properties and environment variables

## Standalone - 1 line configuration

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
---
applicationName: Demo Application
myCredentials:
  username: ${MYAPP_USERNAME:-demoUser}
  password: ${MYAPP_PASSWORD:-}
```

###### build.gradle
```gradle
dependencies {
    compile "com.energizedwork:jfconfig:${jfconfigVersion}"
    
    ....
}
```

## Dropwizard

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

Inherited config will be overridden by the config that inherits it to enable the inherited file to provide both common configuration or default values

A config can only inherit from a single other config, but there are no limits to the size of the inheritance tree
 
Inherited config supports imports in the same way as the main config

If the inheritance key (default "inherits") is found in the config then it must be followed by a location that can be resolved by the ConfigurationSourceProvider with which the config was loaded. The key will be removed from the config and the inherited config will be loaded

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

Imported config does not support inheritance or further imports (any parent/import keys will be removed)

If the import key (default "inherits") is found in the config then the string value will be a location that will be resolved by the ConfigurationSourceProvider with which the config was loaded. The key will be removed from the config and the imported config will be loaded

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
```

#### Advanced import (map)

Advanced options can be enabled by providing a map for the import value.

##### location (required)

The location is required and passed to the ConfigurationSourceProvider to load

##### optional (optional)

If the optional key is not supplied, it defaults to false.
If optional is true, the configuration loading will not fail if the config cannot be loaded

###### config-with-optional-import.yml
```yaml
applicationName: My Application
import:
  location: build-version.yml
  optional: true
```

##### object (optional)

object enables a section of the imported config to be loaded. the value is a dot separated path to the config to import. unless target is specified, the config will be imported to the root of the configuration

TODO e.g.

##### target (optional)

target enables imported config to be relocated to somewhere other than the root of the importing configuration

TODO e.g.

#### Multiple imports

Multiple files can be imported by providing a sequence as the value for the import key.
Simple required imports and the map form can both be used

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

## External configuration file

## System property overrides

## Validation

## Polymophism

## YAML references (and Jackson impl)

## Utilities

### Validating config

### Printing fully resolved config

### Printing config tree before validation

### Printing config tree before validation without env var substitution