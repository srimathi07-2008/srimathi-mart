package com.srimathi.srimathimart.util;

import com.srimathi.srimathimart.exception.AppException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Reads configuration with a clear precedence order so that nothing sensitive
 * ever has to be hardcoded or committed:
 *
 * <ol>
 *   <li>JVM system property (-Ddb.url=...)</li>
 *   <li>Environment variable (DB_URL - dots become underscores, upper case)</li>
 *   <li>config.properties on the classpath</li>
 *   <li>The file named by SRIMATHI_CONFIG, if set</li>
 *   <li>The supplied default</li>
 * </ol>
 */
public final class Config {

    private static final String CONFIG_RESOURCE = "config.properties";
    private static final String CONFIG_PATH_ENV = "SRIMATHI_CONFIG";

    private static final Properties FILE_PROPERTIES = load();

    private Config() {
    }

    private static Properties load() {
        Properties properties = new Properties();

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) {
            loader = Config.class.getClassLoader();
        }

        try (InputStream stream = loader.getResourceAsStream(CONFIG_RESOURCE)) {
            if (stream != null) {
                properties.load(stream);
            }
        } catch (IOException ex) {
            throw new AppException("Unable to read " + CONFIG_RESOURCE, ex);
        }

        String external = System.getenv(CONFIG_PATH_ENV);
        if (external != null && !external.isBlank()) {
            Path path = Paths.get(external);
            if (Files.isReadable(path)) {
                try (InputStream stream = Files.newInputStream(path)) {
                    properties.load(stream);
                } catch (IOException ex) {
                    throw new AppException("Unable to read " + external, ex);
                }
            }
        }

        return properties;
    }

    /**
     * Resolves a configuration key.
     *
     * @param key          dotted property name, for example db.url
     * @param defaultValue value returned when nothing is configured
     * @return the resolved value
     */
    public static String get(final String key, final String defaultValue) {
        String fromSystem = System.getProperty(key);
        if (isSet(fromSystem)) {
            return fromSystem.trim();
        }

        String fromEnv = System.getenv(toEnvName(key));
        if (isSet(fromEnv)) {
            return fromEnv.trim();
        }

        String fromFile = FILE_PROPERTIES.getProperty(key);
        if (isSet(fromFile)) {
            return fromFile.trim();
        }

        return defaultValue;
    }

    /**
     * Resolves a required configuration key.
     *
     * @param key dotted property name
     * @return the resolved value
     */
    public static String require(final String key) {
        String value = get(key, null);
        if (!isSet(value)) {
            throw new AppException("Missing required configuration: " + key
                    + " (set it in config.properties, as the environment variable "
                    + toEnvName(key) + ", or as -D" + key + ")");
        }
        return value;
    }

    /**
     * Resolves an integer configuration key.
     *
     * @param key          dotted property name
     * @param defaultValue fallback when unset or unparseable
     * @return the resolved integer
     */
    public static int getInt(final String key, final int defaultValue) {
        String value = get(key, null);
        if (!isSet(value)) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean isSet(final String value) {
        return value != null && !value.isBlank();
    }

    private static String toEnvName(final String key) {
        return key.replace('.', '_').toUpperCase();
    }
}
