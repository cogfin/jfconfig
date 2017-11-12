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
    bootstrap.setConfigurationFactoryFactory(new DWConfigFactoryFactory<T>('inherits', 'import', 'dw'))
    bootstrap.setConfigurationSourceProvider(JFConfig.createEnvVarSubstitutingClasspathSourceProvider())

    ...
}
```


The above is equivalent to:

```groovy
void initialize(Bootstrap<T> bootstrap) {
    super.initialize(bootstrap)
    bootstrap.setConfigurationFactoryFactory(new DWConfigFactoryFactory<T>('inherits', 'import', 'dw'))
    bootstrap.setConfigurationSourceProvider(
        new SubstitutingSourceProvider(
            new ResourceConfigurationSourceProvider(),
            new EnvironmentVariableSubstitutor(false)
        )
    )

    ...
}
```
