/*
 * Copyright (C) 2026 Robin Kösters
 * mail[at]robinloom[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
