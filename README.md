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
    compile 'com.energizedwork:jfconfig:1.x.y'
    
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

The config that is being inherited will be overriden by the config that is inheriting where the config keys match. This enables the inherited file to provide both common configuration and default values

If the inheritance key (default "inherits") is found in the config then it must be followed by a location that can be resolved by the ConfigurationSourceProvider with which the config was loaded. The key will be removed from the config and the inherited config will be loaded

##### example

###### myapp.yml
```yaml
applicationName: My Application
showStackTrace: false
minifyAssets: true
```

###### cloud.yml
```yaml
inherits: myapp.yml
imageRepository:
  type: aws
  secret: ${AWS_SECRET:-}
  endpoint: ${AWS_ENDPOINT:-xyz.amazonaws.com}
```

###### test.yml
```yaml
inherits: cloud.yml
showStackTrace: true
minifyAssets: false
imageRepository:
  accessKey: ${AWS_ACCESS_KEY:-testUser}
  bucket: test-images
```

###### prod.yml
```yaml
inherits: cloud.yml
imageRepository:
  accessKey: ${AWS_ACCESS_KEY:-prodAwsUser}
  bucket: production-images
```

###### qa.yml
```yaml
inherits: cloud.yml
imageRepository:
  accessKey: ${AWS_ACCESS_KEY:-qaAwsUser}
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

The imported config can be overriden by the config that imports it (or any config that inherits from the importing config)

If the import key (default "inherits") is found in the config then it must be followed by a location that can be resolved by the ConfigurationSourceProvider with which the config was loaded. The key will be removed from the config and the imported config will be loaded

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

#### Optional import

The import can be made optional by providing a map for the import value as below. 

The location is passed to the ConfigurationSourceProvider.
If the optional key is not supplied, it defaults to false.

###### config-with-optional-import.yml
```yaml
applicationName: My Application
import:
  location: build-version.yml
  optional: true
```

#### Multiple imports

Multiple files can be imported by providing a sequence as the value for the import key.
Simple required imports and the map form can both be used

###### multiple-imports.yml
```yaml
applicationName: My Application
import:
  - import-required-config1.yml
  - import-required-config2.yml
  - location: optional-config.yml
    optional: true
  - location: required-config3.yml
    optional: false
```
