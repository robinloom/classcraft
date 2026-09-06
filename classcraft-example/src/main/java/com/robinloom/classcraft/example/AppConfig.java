package com.robinloom.classcraft.example;

import com.robinloom.classcraft.annotations.GenerateBuilder;
import com.robinloom.classcraft.annotations.GenerateDTO;
import com.robinloom.classcraft.annotations.GenerateMapper;
import org.jspecify.annotations.Nullable;

/**
 * Mutable configuration entity.
 * Has no-arg constructor + setters (typical for Spring/Hibernate entities).
 *
 * @GenerateDTO generates AppConfigDTO (also mutable with setters)
 * @GenerateMapper recognizes both are mutable → uses no-arg + setters
 */
@GenerateDTO
@GenerateMapper(to = "com.robinloom.classcraft.example.AppConfigDTO")
public class AppConfig {
    private String appName;
    private String version;
    private String environment;

    // No-arg constructor for mutable class
    public AppConfig() {}

    @GenerateBuilder
    public AppConfig(String appName, String version, @Nullable String environment) {
        this.appName = appName;
        this.version = version;
        this.environment = environment;
    }

    @SuppressWarnings("unused")
    public String getAppName() { return appName; }
    @SuppressWarnings("unused")
    public String getVersion() { return version; }
    @SuppressWarnings("unused")
    public String getEnvironment() { return environment; }

    @SuppressWarnings("unused")
    public void setAppName(String appName) { this.appName = appName; }
    @SuppressWarnings("unused")
    public void setVersion(String version) { this.version = version; }
    @SuppressWarnings("unused")
    public void setEnvironment(String environment) { this.environment = environment; }

    @Override
    public String toString() {
        return "AppConfig{" +
                "appName='" + appName + '\'' +
                ", version='" + version + '\'' +
                ", environment='" + environment + '\'' +
                '}';
    }
}
