/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.plc4x.merlot.logrecorder.servlets.core;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.LoggerFactory;


public class MerlotServiceManagedLogParameters implements ManagedService {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MerlotServiceManagedLogParameters.class);
    private List<Level> levels;
    private List<Tag> tags;
    private List<LogBook> logbooks;
    private List<Property> properties;
    private List<String> templates;

    public MerlotServiceManagedLogParameters() {
        this.levels = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.logbooks = new ArrayList<>();
        this.properties = new ArrayList<>();
        this.templates = new ArrayList<>();
    }

    public List<Level> getLevels() {
        return levels;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public List<LogBook> getLogbooks() {
        return logbooks;
    }

    public List<Property> getProperties() {
        return properties;
    }

    public List<String> getTemplates() {
        return templates;
    }

    
    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        LOGGER.info("Reading properties");
        cleanList();
        converterPropertyLevels((String) properties.get("levels"));
        converterPropertyTagOrLogbook((String) properties.get("tags"), true);
        converterPropertyTagOrLogbook((String) properties.get("logbooks"), false);
    }

    private void converterPropertyLevels(String propertyLevel) {
        LOGGER.info("Reading levels");
        if ((!propertyLevel.isBlank()) && (!propertyLevel.isEmpty()) && (propertyLevel != null)) {

            for (String splitLevel : propertyLevel.split(";")) {
                this.levels.add(new Level("name", splitLevel));
            }

        }
    }

    private void converterPropertyTagOrLogbook(String property, boolean idType) {

        String regexTag = "([^,;]+),([^,;]+)(?=;|$)";
        String regexLogbook = "([^,;]+),([^,;]+),([^,;]+)(?=;|$)";
        Pattern pattern;
        Matcher matcher;

        if ((!property.isBlank()) && (!property.isEmpty()) && (property != null)) {

            if (idType) {
                LOGGER.info("Reading tags");
                pattern = Pattern.compile(regexTag);
                matcher = pattern.matcher(property.trim());

                while (matcher.find()) {
                    String key = matcher.group(1).trim();
                    String state = matcher.group(2).trim();
                    this.tags.add(new Tag(key, state));
                }
            } else {
                LOGGER.info("Reading books");
                pattern = Pattern.compile(regexLogbook);
                matcher = pattern.matcher(property.trim());

                while (matcher.find()) {
                    String name = matcher.group(1).trim();
                    String role = matcher.group(2).trim();
                    String state = matcher.group(3).trim();
                    this.logbooks.add(new LogBook(name, role, state));
                }
            }

        }

    }

    private void cleanList() {
        LOGGER.info("Clean lists");
        this.levels.clear();
        this.tags.clear();
        this.logbooks.clear();
        this.properties.clear();
        this.templates.clear();
    }

 
    public class Level {
        private String key;
        private String description;

        public Level(String key, String description) {
            this.key = key;
            this.description = description;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
        
        
    }


    public class LogBook {

        private String key;
        private String owner;
        private String state;

        public LogBook(String key, String owner, String state) {
            this.key = key;
            this.owner = owner;
            this.state = state;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }    
    }

    
    public class Tag {

        private String key;
        private String state;

        public Tag(String key, String state) {
            this.key = key;
            this.state = state;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }
        
        
    }


    public class Property {

        private String name;
        private String owner;
        private String state;
        private String[] attributes;

        public Property(String name, String owner, String state) {
            this.name = name;
            this.owner = owner;
            this.state = state;
            this.attributes = new String[20];
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String[] getAttributes() {
            return attributes;
        }

        public void setAttributes(String[] attributes) {
            this.attributes = attributes;
        }     
    }

}
