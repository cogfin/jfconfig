package com.energizedwork.justConf;

import io.dropwizard.configuration.ConfigurationException;

/**
 * used to wrap any exceptions from attempting to create a configuration using the {@link JFConfig} utility methods
 *
 * <p>
 *     In particular, the checked exceptions {@link java.io.IOException} and {@link ConfigurationException}.
 * </p>
 */
public class JFConfigException extends RuntimeException {
    JFConfigException(Throwable throwable) {
        super("Failed to create configuration: " + throwable.getLocalizedMessage(), throwable);
    }
    public JFConfigException(String s, Throwable throwable) {
        super(s, throwable);
    }
}
