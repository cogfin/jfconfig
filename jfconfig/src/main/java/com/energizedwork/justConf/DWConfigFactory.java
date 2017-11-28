package com.energizedwork.justConf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
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
import java.util.*;

import static com.fasterxml.jackson.dataformat.yaml.YAMLFactory.FORMAT_NAME_YAML;
import static java.util.Objects.requireNonNull;

/**
 * a dropwizard configurationFactory supporting configuration inheritance, imports, optional external configuration file
 * and system property overrides
 *
 * @param <T> the class that will be configured
 */
public class DWConfigFactory<T> extends BaseConfigurationFactory<T> {

    /**
     * {@value}
     */
    public static final String IMPORT_KEY_LOCATION = "location";

    /**
     * {@value}
     */
    public static final String IMPORT_KEY_OPTIONAL = "optional";

    /**
     * {@value}
     */
    public static final String IMPORT_KEY_SUB_TREE = "object";

    /**
     * {@value}
     */
    public static final String IMPORT_KEY_TARGET = "target";

    /**
     * {@value}
     */
    public static final String OBJECT_PATH_SEPARATOR = ".";

    private static final String OBJECT_PATH_SEPARATOR_REGEX = "\\.";

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

    /**
     * create the configuration tree (with inheritance, imports, etc) and return before mapping onto the configuration object
     * and validating
     *
     * @param provider the configurationSourceProvider to use to load configuration files
     * @param path the path to the configuration that will be understood by the provider
     * @return the config tree
     * @throws IOException
     * @throws ConfigurationException
     */
    public ObjectNode buildTree(ConfigurationSourceProvider provider, String path) throws IOException, ConfigurationException {
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
        return mergeFromImportNode(sourceProvider, importer, importNode, null, null);
    }

    ObjectNode mergeFromImportNode(ConfigurationSourceProvider sourceProvider, ObjectNode importer, JsonNode importNode, String sourceRoot, String destTarget) throws DWConfigFactoryException {
        if (importNode.isTextual() && importNode.asText() != null) {
            ObjectNode in = readTree(sourceProvider, importNode.asText());
            in.remove(importKey);
            in.remove(parentKey);
            return mergeAndReturnDest(importer, moveTarget(getSubObject(in, sourceRoot), destTarget));
        } else if (importNode.isObject() && importNode.get(IMPORT_KEY_LOCATION) != null && importNode.get(IMPORT_KEY_LOCATION).isTextual()) {
            JsonNode optionalNode = importNode.get(IMPORT_KEY_OPTIONAL);
            String importRoot = readNode(importNode, IMPORT_KEY_SUB_TREE);
            String importTarget = readNode(importNode, IMPORT_KEY_TARGET);
            if (optionalNode != null && optionalNode.isBoolean() && optionalNode.asBoolean()) {
                return importOptionalConfig(sourceProvider, importer, importNode.get(IMPORT_KEY_LOCATION), importRoot, importTarget);
            } else {
                return mergeFromImportNode(sourceProvider, importer, importNode.get(IMPORT_KEY_LOCATION), importRoot, importTarget);
            }
        } else {
            return importer;
        }
    }

    private String readNode(JsonNode node, String key) {
        JsonNode optionalNode = node.get(key);
        return optionalNode != null && optionalNode.isTextual() ? optionalNode.asText() : null;
    }

    private ObjectNode moveTarget(ObjectNode importTree, String destTarget) {
        if (destTarget == null) {
            return importTree;
        } else {
            JsonNodeFactory factory = JsonNodeFactory.instance;
            ObjectNode root = factory.objectNode();
            ObjectNode current = root;
            String[] targetObjects = destTarget.split(OBJECT_PATH_SEPARATOR_REGEX);
            String[] allBytLast = Arrays.copyOf(targetObjects, targetObjects.length -1);
            for (String object : allBytLast) {
                ObjectNode newNode = factory.objectNode();
                current.set(object, newNode);
                current = newNode;
            }
            current.set(targetObjects[targetObjects.length -1], importTree);
            return root;
        }
    }

    private ObjectNode getSubObject(ObjectNode importTree, String objectPath) throws DWConfigFactoryException {
        if (objectPath == null) {
            return importTree;
        } else if (objectPath.contains(OBJECT_PATH_SEPARATOR)) {
            String[] headAndTail = objectPath.split(OBJECT_PATH_SEPARATOR_REGEX, 2);
            ObjectNode objectNode = getObject(importTree, headAndTail[0]);
            return getSubObject(objectNode, headAndTail[1]);
        } else {
            return getObject(importTree, objectPath);
        }
    }

    private ObjectNode getObject(ObjectNode tree, String path) throws DWConfigFactoryException {
        JsonNode jsonNode = tree.get(path);
        if (jsonNode == null || !jsonNode.isObject()) {
            throw new DWConfigFactoryException("Could not find object in imported config", path, configPaths);
        } else {
            return (ObjectNode) jsonNode;
        }
    }

    private ObjectNode importOptionalConfig(ConfigurationSourceProvider sourceProvider, ObjectNode importer, JsonNode locationNode, String importRoot, String destTarget) {
        try {
            return mergeFromImportNode(sourceProvider, importer, locationNode, importRoot, destTarget);
        } catch (Exception e) {
            log.debug("Failed to read optional config {}", locationNode.asText(), e);
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
            throw new DWConfigFactoryException("Failed to open config file", path, addIfNotAlreadyAdded(configPaths, path), e);
        }
        try {
            ObjectNode on = mapper.readTree(createParser(configIs));
            if (on == null) {
                throw new DWConfigFactoryException("Failed to read config file", path, addIfNotAlreadyAdded(configPaths, path));
            }
            return on;
        } catch (IOException e) {
            throw new DWConfigFactoryException(path, addIfNotAlreadyAdded(configPaths, path), e);
        } finally {
            try {
                configIs.close();
            } catch (Exception e) {
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

    private static List<String> addIfNotAlreadyAdded(List<String> source, String newPath) {
        if (source.get(source.size() - 1).equals(newPath)) {
            return source;
        }
        List<String> newList = new LinkedList<String>();
        newList.addAll(source);
        newList.add(newPath);
        return newList;
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
