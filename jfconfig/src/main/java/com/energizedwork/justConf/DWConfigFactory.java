package com.energizedwork.justConf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.dropwizard.configuration.BaseConfigurationFactory;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.configuration.FileConfigurationSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.fasterxml.jackson.dataformat.yaml.YAMLFactory.FORMAT_NAME_YAML;
import static java.util.Objects.requireNonNull;

/**
 * a dropwizard configurationFactory supporting configuration inheritance, imports, optional external configuration file
 * and system property overrides
 *
 * @param <T> the class that will be configured
 */
public class DWConfigFactory<T> extends BaseConfigurationFactory<T> {

    final Logger log = LoggerFactory.getLogger(DWConfigFactory.class);
    final String parentKey;
    final String importKey;
    final File externalConfigFile;
    final List<String> configPaths = new ArrayList<String>();

    /**
     * @param klass the class to configure
     * @param validator the validator to ensure the configuration has been fully configured
     * @param objectMapper the objectMapper used to bind the yaml config to the cofiguration instance
     * @param propertyPrefix the prefix for system properties used to override configuration
     * @param parentKey the key in the yaml document to identify a parent configuration
     * @param importKey the key in the yaml document to identify configurations to import. When null, imports are disabled.
     * @param externalConfigFile an optional external configuration file for overriding config. Set to null when not required.
     *                           The file does not need to exist
     */
    public DWConfigFactory(Class<T> klass,
                           Validator validator,
                           ObjectMapper objectMapper,
                           String propertyPrefix,
                           String parentKey,
                           String importKey,
                           File externalConfigFile) {
        super(new YAMLFactory(), FORMAT_NAME_YAML, klass, validator, objectMapper, propertyPrefix);
        this.parentKey = parentKey;
        this.importKey = importKey;
        this.externalConfigFile = externalConfigFile;
    }

    /**
     * create and validate a configuration object
     *
     * @param provider the configurationSourceProvider to use to load configuration files
     * @param path the path to the configuration that will be understood by the provider
     * @return the configuration
     * @throws IOException
     * @throws ConfigurationException
     */
    @Override
    public T build(ConfigurationSourceProvider provider, String path) throws IOException, ConfigurationException {
        return super.build(buildTree(provider, path), path);
    }

    ObjectNode buildTree(ConfigurationSourceProvider provider, String path) throws IOException, ConfigurationException {
        ObjectNode externalConfig = null;
        if (externalConfigFile != null) {
            String externalPath = externalConfigFile.getCanonicalPath();
            if (externalConfigFile.exists()) {
                configPaths.add(externalPath);
                log.debug("Loading external config file '{}'", externalPath);
                externalConfig = readTree(new FileConfigurationSourceProvider(), externalPath);
            } else {
                log.debug("External config file '{}' does not exist, ignoring", externalPath);
            }
        }
        configPaths.add(path);
        ObjectNode topConfigNode = readTree(provider, requireNonNull(path));
        topConfigNode = importFromProvider(provider, topConfigNode);
        if (externalConfig != null) {
            merge(externalConfig, topConfigNode);
        }
        return mergeParents(provider, topConfigNode);
    }

    private ObjectNode importFromProvider(ConfigurationSourceProvider sourceProvider, ObjectNode importer) throws DWConfigFactoryException {
        if (importKey == null) {
            return importer;
        }
        JsonNode importNode = importer.remove(importKey);
        if (importNode == null) {
            return importer;
        }
        if (importNode.isTextual() || importNode.isObject()) {
            return mergeFromImportNode(sourceProvider, importer, importNode);
        } else if (importNode.isArray()) {
            Iterator<JsonNode> it = importNode.elements();
            ObjectNode merging = importer;
            while (it.hasNext()) {
                JsonNode jn = it.next();
                merging = mergeFromImportNode(sourceProvider, merging, jn);
            }
            return merging;
        } else {
            return importer;
        }
    }

    ObjectNode mergeFromImportNode(ConfigurationSourceProvider sourceProvider, ObjectNode importer, JsonNode importNode) throws DWConfigFactoryException {
        if (importNode.isTextual() && importNode.asText() != null) {
            return mergeAndReturnDest(importer, readTree(sourceProvider, importNode.asText()));
        } else if (importNode.isObject() && importNode.get("location") != null && importNode.get("location").isTextual()) {
            JsonNode optionalNode = importNode.get("optional");
            if (optionalNode != null && optionalNode.isBoolean() && optionalNode.asBoolean()) {
                return importOptionalConfig(sourceProvider, importer, importNode);
            } else {
                return mergeFromImportNode(sourceProvider, importer, importNode.get("location"));
            }
        } else {
            return importer;
        }
    }

    private ObjectNode importOptionalConfig(ConfigurationSourceProvider sourceProvider, ObjectNode importer, JsonNode importNode) {
        try {
            return mergeFromImportNode(sourceProvider, importer, importNode.get("location"));
        } catch (DWConfigFactoryException e) {
            log.debug("Failed to read optional config {}", importNode.get("location").asText(), e);
            return importer;
        }
    }

    private ObjectNode mergeParents(ConfigurationSourceProvider sourceProvider, ObjectNode config) throws DWConfigFactoryException {
        JsonNode parentPathNode = config.remove(parentKey);
        if (parentPathNode != null && parentPathNode.asText() != null) {
            ObjectNode parent = readParent(sourceProvider, parentPathNode.asText());
            parent = importFromProvider(sourceProvider, parent);
            merge(config, parent);
            return mergeParents(sourceProvider, parent);
        } else {
            return config;
        }
    }

    private ObjectNode readParent(ConfigurationSourceProvider sourceProvider, String path) throws DWConfigFactoryException {
        if (configPaths.contains(path)) {
            configPaths.add(path);
            throw new DWConfigFactoryException("Circular inheritance", path, configPaths);
        }
        configPaths.add(path);
        return readTree(sourceProvider, path);
    }

    private ObjectNode readTree(ConfigurationSourceProvider sourceProvider, String path) throws DWConfigFactoryException {
        InputStream configIs;
        try {
            configIs = sourceProvider.open(path);
        } catch (Exception e) {
            throw new DWConfigFactoryException("Failed to open config file", path, configPaths, e);
        }
        try {
            return mapper.readTree(createParser(configIs));
        } catch (IOException e) {
            throw new DWConfigFactoryException(path, configPaths, e);
        } finally {
            try {
                configIs.close();
            } catch (IOException e) {
                log.debug("Closing input stream from " + path, e);
            }
        }
    }

    private ObjectNode mergeAndReturnDest(ObjectNode sourceNode, ObjectNode destNode) {
        merge(sourceNode, destNode);
        return destNode;
    }

    private void merge(JsonNode sourceNode, JsonNode destNode) {
        if (sourceNode != null) {
            Iterator<String> fieldNames = sourceNode.fieldNames();
            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode sourceFieldValue = sourceNode.get(fieldName);
                JsonNode destFieldValue = destNode.get(fieldName);
                if (destFieldValue != null && destFieldValue.isObject()) {
                    merge(sourceFieldValue, destFieldValue);
                } else if (destNode instanceof ObjectNode) {
                    ((ObjectNode) destNode).replace(fieldName, sourceFieldValue);
                }
            }
        }
    }

    static class DWConfigFactoryException extends ConfigurationException {
        DWConfigFactoryException(String path, List<String> paths, Throwable cause) {
            super(path, buildConfigInheritanceString("", paths), cause);
        }
        DWConfigFactoryException(String error, String path, List<String> paths) {
            super(path, buildConfigInheritanceString(error + ": ", paths));
        }
        DWConfigFactoryException(String error, String path, List<String> paths, Throwable cause) {
            super(path, buildConfigInheritanceString(error + ": ", paths), cause);
        }
        static List<String> buildConfigInheritanceString(String prefix, List<String> paths) {
            StringBuilder sb = new StringBuilder(prefix);
            paths.forEach(s -> sb.append(s).append(" -> "));
            return Collections.singletonList(sb.substring(0, sb.length() - 4));
        }
    }

}
