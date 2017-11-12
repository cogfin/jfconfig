package com.energizedwork.justConf;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import io.dropwizard.configuration.*;

import java.io.File;
import java.io.OutputStream;

import static com.energizedwork.justConf.DWConfigFactoryFactory.DEFAULT_IMPORT_KEY;
import static com.energizedwork.justConf.DWConfigFactoryFactory.DEFAULT_PARENT_KEY;
import static com.energizedwork.justConf.DWConfigFactoryFactory.DEFAULT_PROPERTY_OVERRIDE_PREFIX;
import static io.dropwizard.jackson.Jackson.newObjectMapper;
import static io.dropwizard.jersey.validation.Validators.newValidatorFactory;

/**
 * utility methods for standalone configuration and bootstrap helpers
 *
 * <p>
 * All methods that return a configuration object may throw a {@link JFConfigException}
 * </p>
 */
public class JFConfig {

    /**
     * wrap a configurationSourceProvider with a substitutingSourceProvider for environment variable replacement using the
     * environmentVariableSubstitutor based on the apache commons StrSubstitutor
     *
     * @param sourceProvider the configuration sourceProvider to wrap
     * @return a sourcePorvider that will replace environment variables
     */
    public static ConfigurationSourceProvider envVarSubstitution(ConfigurationSourceProvider sourceProvider) {
        return new SubstitutingSourceProvider(
                sourceProvider,
                new EnvironmentVariableSubstitutor(false)
        );
    }

    /**
     * create a file source provider with environment variable substitution
     *
     * @return a fileConfigurationSourceProvider with environment variable substitution
     */
    public static ConfigurationSourceProvider createEnvVarSubstitutingFileSourceProvider() {
        return envVarSubstitution(new FileConfigurationSourceProvider());
    }

    /**
     * create a url source provider with environment variable substitution
     *
     * @return a urlConfigurationSourceProvider with environment variable substitution
     */
    public static ConfigurationSourceProvider createEnvVarSubstitutingUrlSourceProvider() {
        return envVarSubstitution(new UrlConfigurationSourceProvider());
    }

    /**
     * create a classpath source provider with environment variable substitution
     *
     * @return a resourceConfigurationSourceProvider with environment variable substitution
     */
    public static ConfigurationSourceProvider createEnvVarSubstitutingClasspathSourceProvider() {
        return envVarSubstitution(new ResourceConfigurationSourceProvider());
    }

    /**
     * configure from a file on the filesystem
     *
     * @param configClass the class of the object to create and configure
     * @param configPath the location of the configuration file
     * @param <C> the class of the object to create and configure
     * @return a configuration object
     */
    public static <C> C fromFile(Class<C> configClass, String configPath) {
        return fromSourceProvider(createEnvVarSubstitutingFileSourceProvider(), configClass, configPath);
    }

    /**
     * configure from a file on the filesystem and provide an optional external config file location
     * for overrides
     *
     * @param configClass the class of the object to create and configure
     * @param configPath the location of the configuration file
     * @param externalConfigFile an external configuration file for overrides
     * @param <C> the class of the object to create and configure
     * @return a configuration object
     */
    public static <C> C fromFile(Class<C> configClass, String configPath, File externalConfigFile) {
        return fromSourceProvider(createEnvVarSubstitutingFileSourceProvider(), configClass, configPath, externalConfigFile);
    }

    /**
     * configure from a file on the classpath
     *
     * <p>
     * e.g. {@code conf/demo.yml}
     * </p>
     *
     * @param configClass the class of the object to create and configure
     * @param configPath the location of the configuration file on the classpath
     * @param <C> the class of the object to create and configure
     * @return a configuration object
     */
    public static <C> C fromClasspath(Class<C> configClass, String configPath) {
        return fromSourceProvider(createEnvVarSubstitutingClasspathSourceProvider(), configClass, configPath);
    }

    /**
     * configure from a file on the classpath with an optional file for overrides
     *
     * <p>
     * e.g. {@code conf/demo.yml}
     * </p>
     *
     * @param configClass the class of the object to create and configure
     * @param configPath the location of the configuration file on the classpath
     * @param externalConfigFile an external configuration file for overrides
     * @param <C> the class of the object to create and configure
     * @return a configuration object
     */
    public static <C> C fromClasspath(Class<C> configClass, String configPath, File externalConfigFile) {
        return fromSourceProvider(createEnvVarSubstitutingClasspathSourceProvider(), configClass, configPath, externalConfigFile);
    }

    /**
     * create a configuration using any configurationSourceProvider
     *
     * <p>
     * Use this utility method when you know you do not want environment variable substitution to speed up configuration
     * loading
     * </p>
     *
     * @param sourceProvider the source provider used to retrieve the configuration from the configLocation
     * @param configClass the class of the object to create and configure
     * @param configLocation the location of the configuration (which will be understood by the configurationSourceProvider)
     * @param <C> the class of the object to create and configure
     * @return a configuration object created from the provided configurationSourceProvider
     */
    public static <C> C fromSourceProvider(ConfigurationSourceProvider sourceProvider, Class<C> configClass, String configLocation) {
        return fromSourceProvider(sourceProvider, configClass, configLocation, DEFAULT_PARENT_KEY, DEFAULT_IMPORT_KEY, null, DEFAULT_PROPERTY_OVERRIDE_PREFIX);
    }

    /**
     * create a configuration using any configurationSourceProvider and provide an optional external config file location
     * for overrides
     *
     * <p>
     * Use this utility method when you know you do not want environment variable substitution to speed up configuration
     * loading
     * </p>
     *
     * @param sourceProvider the source provider used to retrieve the configuration from the configLocation
     * @param configClass the class of the object to create and configure
     * @param configLocation the location of the configuration (which will be understood by the configurationSourceProvider)
     * @param externalConfigFile an optional external configuration file for overrides
     * @param <C> the class of the object to create and configure
     * @return a configuration object created from the provided configurationSourceProvider
     */
    public static <C> C fromSourceProvider(ConfigurationSourceProvider sourceProvider, Class<C> configClass, String configLocation, File externalConfigFile) {
        return fromSourceProvider(sourceProvider, configClass, configLocation, DEFAULT_PARENT_KEY, DEFAULT_IMPORT_KEY, externalConfigFile, DEFAULT_PROPERTY_OVERRIDE_PREFIX);
    }

    /**
     * create a configuration without using any defaults
     *
     * @param sourceProvider the source provider used to retrieve the configuration from the configLocation
     * @param configClass the class of the object to create and configure
     * @param configLocation the location of the configuration (which will be understood by the configurationSourceProvider)
     * @param parentKey the key in the yaml document to identify a parent configuration
     * @param importKey the key in the yaml document to identify configurations to import. When null, imports are disabled.
     * @param externalConfigFile an optional external configuration file for overrides
     * @param propertyOverridePrefix prefix for system property overrides
     * @param <C> the class of the object to create and configure
     * @return a configuration object
     */
    public static <C> C fromSourceProvider(ConfigurationSourceProvider sourceProvider, Class<C> configClass, String configLocation, String parentKey, String importKey, File externalConfigFile, String propertyOverridePrefix) {
        try {
            DWConfigFactoryFactory<C> factoryFactory = new DWConfigFactoryFactory<C>(parentKey, importKey, propertyOverridePrefix, externalConfigFile);
            ConfigurationFactory<C> factory = factoryFactory.create(configClass, newValidatorFactory().getValidator(), newObjectMapper(), "IGNORED");
            return factory.build(sourceProvider, configLocation);
        } catch (Exception e) {
            throw new JFConfigException(e);
        }
    }

    /**
     * write an object as yaml to the outputStream provided
     *
     * <p>
     *     Convenience method for printing fully resolved configurations once they have been loaded
     * </p>
     *
     * @param out the outputStream to write the serialized object
     * @param configuration an object to serialize
     */
    public static void printConfig(OutputStream out, Object configuration) {
        try {
            createYamlObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out, configuration);
        } catch (Exception e) {
            throw new JFConfigException("Failed to serialize configuration", e);
        }
    }

    /**
     * print a resolved configuration before it is mapped to a configuration object and validated
     *
     * <p>
     *     When using env var substitution for configuration, calling this without the substitution source provider will
     *     display the env vars that would get replaced.
     * </p>
     *
     * @param out the outputStream to write the raw config
     * @param sourceProvider the source provider used to retrieve the configuration from the configLocation
     * @param configLocation the location of the configuration (which will be understood by the configurationSourceProvider)
     */
    public static void printConfigTree(OutputStream out, ConfigurationSourceProvider sourceProvider, String configLocation) {
        printConfigTree(out, sourceProvider, configLocation, DEFAULT_PARENT_KEY, DEFAULT_IMPORT_KEY, null);
    }

    /**
     * print a resolved configuration before it is mapped to a configuration object and validated
     *
     * <p>
     *     When using env var substitution for configuration, calling this without the substitution source provider will
     *     display the env vars that would get replaced.
     * </p>
     *
     * @param out the outputStream to write the raw config
     * @param sourceProvider the source provider used to retrieve the configuration from the configLocation
     * @param configLocation the location of the configuration (which will be understood by the configurationSourceProvider)
     * @param parentKey the key in the yaml document to identify a parent configuration
     * @param importKey the key in the yaml document to identify configurations to import. When null, imports are disabled.
     */
    public static void printConfigTree(OutputStream out, ConfigurationSourceProvider sourceProvider, String configLocation, String parentKey, String importKey) {
        printConfigTree(out, sourceProvider, configLocation, parentKey, importKey, null);
    }

    /**
     * print a resolved configuration before it is mapped to a configuration object and validated
     *
     * <p>
     *     When using env var substitution for configuration, calling this without the substitution source provider will
     *     display the env vars that would get replaced.
     * </p>
     *
     * @param out the outputStream to write the raw config
     * @param sourceProvider the source provider used to retrieve the configuration from the configLocation
     * @param configLocation the location of the configuration (which will be understood by the configurationSourceProvider)
     * @param parentKey the key in the yaml document to identify a parent configuration
     * @param importKey the key in the yaml document to identify configurations to import. When null, imports are disabled.
     * @param externalConfigFile an optional external configuration file for overrides
     */
    @SuppressWarnings("unchecked")
    public static void printConfigTree(OutputStream out, ConfigurationSourceProvider sourceProvider, String configLocation, String parentKey, String importKey, File externalConfigFile) {
        try {
            DWConfigFactoryFactory<Object> factoryFactory = new DWConfigFactoryFactory<Object>(parentKey, importKey, "N/A", externalConfigFile);
            DWConfigFactory factory = (DWConfigFactory) factoryFactory.create(Object.class, newValidatorFactory().getValidator(), newObjectMapper(), "N/A");
            createYamlObjectMapper().writerWithDefaultPrettyPrinter().writeValue(out, factory.buildTree(sourceProvider, configLocation));
        } catch (Exception e) {
            throw new JFConfigException(e);
        }
    }

    private static ObjectMapper createYamlObjectMapper() {
        YAMLFactory factory = new YAMLFactory().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        return new ObjectMapper(factory);
    }

    private JFConfig() {}

}
