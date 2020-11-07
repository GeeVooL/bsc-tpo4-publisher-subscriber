package com.mdevv.tpo4.common;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class Configuration {
    final List<String> requiredKeys;
    private Map<String, String> configurationMap = new HashMap<>();

    public Configuration(String filePath, List<String> requiredKeys) throws IOException {
        this.requiredKeys = requiredKeys;
        try (Stream<String> stream = Files.lines(Paths.get(filePath), StandardCharsets.UTF_8)) {
            stream.forEach(s -> {
                String[] parts = s.split("=");

                if (parts.length != 2) {
                    throw new IllegalArgumentException("Invalid configuration format");
                }

                String key = parts[0];
                String value = parts[1];
                if (!requiredKeys.contains(key)) {
                    throw new IllegalArgumentException("Invalid configuration key");
                }

                configurationMap.put(key, value);
            });
        }

        if (!configurationMap.keySet().containsAll(requiredKeys)) {
            throw new IllegalArgumentException("Partial configuration is not accepted");
        }
    }

    public String get(String item) {
        return configurationMap.get(item);
    }

    public Integer getAsInt(String item) {
        String value = configurationMap.get(item);
        return value != null ? Integer.valueOf(value) : null;
    }

    public static Configuration fromCommandLine(String[] args, List<String> requiredOptions) throws IOException {
        String configurationPath = "";

        if (args.length > 0) {
            configurationPath = args[0];
        } else {
            System.err.println("This program requires one parameter - a path to configuration file.");
            System.exit(1);
        }

        return new Configuration(configurationPath, requiredOptions);
    }
}
