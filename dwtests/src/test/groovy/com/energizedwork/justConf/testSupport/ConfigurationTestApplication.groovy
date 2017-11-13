package com.energizedwork.justConf.testSupport

import io.dropwizard.Application
import io.dropwizard.cli.ConfiguredCommand
import io.dropwizard.configuration.EnvironmentVariableSubstitutor
import io.dropwizard.configuration.ResourceConfigurationSourceProvider
import io.dropwizard.configuration.SubstitutingSourceProvider
import io.dropwizard.setup.Bootstrap
import io.dropwizard.setup.Environment
import com.energizedwork.justConf.DWConfigFactoryFactory
import net.sourceforge.argparse4j.inf.Namespace

import static com.energizedwork.justConf.DWConfigFactoryFactory.DEFAULT_PROPERTY_OVERRIDE_PREFIX

class ConfigurationTestApplication<T> extends Application<T> {
    def configCommand
    def configClass
    def importKey
    def externalConfig
    ConfigurationTestApplication(Class<? extends T> configClass, String importKey = null, File externalConfig = null) {
        this.configClass = configClass
        this.importKey = importKey
        this.externalConfig = externalConfig
    }
    @Override
    void initialize(Bootstrap<T> bootstrap) {
        super.initialize(bootstrap)
        this.configCommand = new ConfigCommand(this)
        bootstrap.setConfigurationFactoryFactory(new DWConfigFactoryFactory<T>("inherits", importKey, DEFAULT_PROPERTY_OVERRIDE_PREFIX, externalConfig))
        bootstrap.addCommand(configCommand)
        bootstrap.setConfigurationSourceProvider(
                new SubstitutingSourceProvider(
                        new ResourceConfigurationSourceProvider(),
                        new EnvironmentVariableSubstitutor(false)
                )
        )
    }
    @Override
    void run(T configuration, Environment environment) throws Exception {
    }
    @Override
    Class<T> getConfigurationClass() {
        configClass
    }
    T getConfiguration() {
        configCommand.configuration
    }

    @Override
    protected void onFatalError() {
        throw new FatalErrorDetectedException()
    }

    static class ConfigCommand extends ConfiguredCommand {
        def application
        def configuration

        ConfigCommand(Application application) {
            super('configure', 'Configure and provide access to config')
            this.application = application
        }

        @Override
        protected Class getConfigurationClass() {
            application.configurationClass
        }

        @Override
        protected void run(Bootstrap bootstrap, Namespace namespace, configuration) throws Exception {
            this.configuration = configuration
        }
    }

    static class FatalErrorDetectedException extends Exception {
    }

}
