package com.mobilesales.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Utility to securely load environment variables from .env files or system environment.
 * Ensures secret keys (such as AI_API_KEY) are never exposed to the frontend or version control.
 */
public class EnvConfig {

    private static final Logger LOGGER = Logger.getLogger(EnvConfig.class.getName());
    private static final Map<String, String> ENV_CACHE = new HashMap<>();
    private static boolean isLoaded = false;

    static {
        load();
    }

    public static synchronized void load() {
        if (isLoaded) return;
        
        File envFile = new File(".env");
        if (envFile.exists() && envFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int equalsIdx = line.indexOf('=');
                    if (equalsIdx > 0) {
                        String key = line.substring(0, equalsIdx).trim();
                        String val = line.substring(equalsIdx + 1).trim();
                        // Remove surrounding quotes if any
                        if ((val.startsWith("\"") && val.endsWith("\"")) ||
                            (val.startsWith("'") && val.endsWith("'"))) {
                            if (val.length() >= 2) {
                                val = val.substring(1, val.length() - 1);
                            }
                        }
                        ENV_CACHE.put(key, val);
                    }
                }
                LOGGER.info(".env configuration file loaded successfully.");
            } catch (IOException e) {
                LOGGER.warning("Could not read .env file: " + e.getMessage());
            }
        }
        isLoaded = true;
    }

    /**
     * Retrieves an environment value by key with fallback to system env, system property, and .env.
     */
    public static String get(String key) {
        return get(key, null);
    }

    public static String get(String key, String defaultValue) {
        load();
        
        // 1. Check System environment
        String sysVal = System.getenv(key);
        if (sysVal != null && !sysVal.trim().isEmpty()) {
            return sysVal.trim();
        }

        // 2. Check System properties
        String propVal = System.getProperty(key);
        if (propVal != null && !propVal.trim().isEmpty()) {
            return propVal.trim();
        }

        // 3. Check loaded .env file
        String envVal = ENV_CACHE.get(key);
        if (envVal != null && !envVal.trim().isEmpty()) {
            return envVal.trim();
        }

        return defaultValue;
    }

    public static boolean hasApiKey() {
        String key = get("AI_API_KEY");
        return key != null && !key.trim().isEmpty() && !key.equalsIgnoreCase("your_api_key_here");
    }
}
