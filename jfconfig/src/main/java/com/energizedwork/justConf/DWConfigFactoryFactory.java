package com.energizedwork.justConf;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;

import javax.validation.Validator;
import java.io.File;
import java.util.Collections;
import java.util.List;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

/**
 * the dropwizardConfigurationFactoryFactory
 *
 * <p>
 * Set an instance of this class on the Bootstrap object to use the dropwizardConfigurationFactory.
 * </p>
 *
 * @param <T> The configuration class
 */
public class DWConfigFactoryFactory<T> implements ConfigurationFactoryFactory<T> {

    /**
     * {@value}
     */
    public static final String DEFAULT_PARENT_KEY = "inherits";
    /**
     * {@value}
     */
    public static final String DEFAULT_IMPORT_KEY = "import";
    /**
     * {@value}
     */
    public static final String DEFAULT_PROPERTY_OVERRIDE_PREFIX = "jf-conf";

    private final String parentKey;
    private final String importKey;
    private final String propertyOverridePrefix;
    private final File externalConfig;
    private final List<DeserializationFeature> enableFeatures;
    private final List<DeserializationFeature> disableFeatures;

    /**
     * A factory that creates {@link DWConfigFactory} with default parentKey, importKey and propertyOverridePrefix, no
     * external configuration file and the {@link ObjectMapper} set to {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}
     */
    public DWConfigFactoryFactory() {
        this(DEFAULT_PARENT_KEY, DEFAULT_IMPORT_KEY, DEFAULT_PROPERTY_OVERRIDE_PREFIX);
    }

    /**
     * A factory that creates {@link DWConfigFactory} with no external configuration file and the {@link ObjectMapper}
     * set to {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}
     *
     * @param parentKey the key in the yaml document to identify a parent configuration
     * @param importKey the key in the yaml document to identify configurations to import. When null, imports are disabled.
     * @param propertyOverridePrefix the string prefix for system properties to identify them as configuration overrides
     */
    public DWConfigFactoryFactory(String parentKey, String importKey, String propertyOverridePrefix) {
        this(parentKey, importKey, propertyOverridePrefix, null, Collections.singletonList(FAIL_ON_UNKNOWN_PROPERTIES), Collections.emptyList());
    }

    /**
     * A factory that creates {@link DWConfigFactory} with an optional external configuration file and the {@link ObjectMapper}
     * set to {@link DeserializationFeature#FAIL_ON_UNKNOWN_PROPERTIES}
     *
     * @param parentKey the key in the yaml document to identify a parent configuration
     * @param importKey the key in the yaml document to identify configurations to import. When null, imports are disabled.
     * @param propertyOverridePrefix the string prefix for system properties to identify them as configuration overrides
     * @param externalConfig a file which if present provides overriding configuration (YAML)
     */
    public DWConfigFactoryFactory(String parentKey, String importKey, String propertyOverridePrefix, File externalConfig) {
        this(parentKey, importKey, propertyOverridePrefix, externalConfig, Collections.singletonList(FAIL_ON_UNKNOWN_PROPERTIES), Collections.emptyList());
    }

    /**
     * A factory that creates {@link DWConfigFactory} with an optional external configuration file and {@link DeserializationFeature}s
     * to enable and disable on the {@link ObjectMapper}
     *
     * @param parentKey the key in the yaml document to identify a parent configuration
     * @param importKey the key in the yaml document to identify configurations to import. When null, imports are disabled.
     * @param propertyOverridePrefix the string prefix for system properties to identify them as configuration overrides
     * @param externalConfig a file which if present provides overriding configuration (YAML)
     * @param enableFeatures a list of deserializationFeatures to enable on the objectMapper
     * @param disableFeatures a list of deserializationFeatures to disable on the objectMapper
     */
    public DWConfigFactoryFactory(String parentKey, String importKey, String propertyOverridePrefix, File externalConfig,
                                  List<DeserializationFeature> enableFeatures,
                                  List<DeserializationFeature> disableFeatures) {
        this.parentKey = parentKey;
        this.importKey = importKey;
        this.propertyOverridePrefix = propertyOverridePrefix;
        this.externalConfig = externalConfig;
        this.enableFeatures = enableFeatures;
        this.disableFeatures = disableFeatures;
    }

    /**
     * the factory method to create the dropwizardConfigurationFactory
     *
     * <p>
     * Invoked by dropwizard
     * </p>
     */
    @Override
    public ConfigurationFactory<T> create(
            Class<T>     klass,
            Validator    validator,
            ObjectMapper objectMapper,
            String       IGNORED_PROPERTY_PREFIX) {
        return new DWConfigFactory<T>(
            klass,
            validator,
            configureObjectMapper(objectMapper.copy()),
            propertyOverridePrefix,
            parentKey,
            importKey,
            externalConfig);
    }

    private ObjectMapper configureObjectMapper(ObjectMapper objectMapper) {
        enableFeatures.forEach(deserializationFeature -> { objectMapper.enable(deserializationFeature); });
        disableFeatures.forEach(deserializationFeature -> { objectMapper.disable(deserializationFeature); });
        return objectMapper;
    }

}
