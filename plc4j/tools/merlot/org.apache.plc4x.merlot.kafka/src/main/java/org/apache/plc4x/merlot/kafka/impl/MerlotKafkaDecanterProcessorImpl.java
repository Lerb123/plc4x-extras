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
package org.apache.plc4x.merlot.kafka.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlotKafkaDecanterProcessorImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(MerlotKafkaDecanterProcessorImpl.class);

    private static final Pattern TOPIC_ALARM_PATTERN = Pattern.compile(":/([^/]+)/");
    private static final Pattern COMPONENT_PATTERN = Pattern.compile("^[^:/]+:/[^/]+/(.+)/[a-zA-Z0-9]+:[\\\\/]{2}");
    private static final String ESCAPED_PROTOCOL_SEPARATOR = ":\\/\\/";
    private static final String PROTOCOL_SEPARATOR = "://";
    private ObjectMapper mapper = new ObjectMapper();
    private final EventAdmin dispatcher;

    public MerlotKafkaDecanterProcessorImpl(EventAdmin dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void processRecord(String eventAdminTopic, ConsumerRecord<String, String> record) {
        
        try {
            Map<String, Object> data = new HashMap<>(12);

            data.put("loki.label.job", "MerlotAlarmCollector");

            //Data headers
            String key = record.key();

            //Alarm values
            String value = record.value();
            JsonNode rootNode = mapper.readTree(value);
            JsonNode timeNode = rootNode.get("time");
            
            //LOGGER.info("Key: {} Value: {}", key, value);
            String pathPV = getPathPV(key);
            
            
            
            //Loki paramaters
            data.put("loki.label.topicalarm", getDataBasedOnPattern(key, TOPIC_ALARM_PATTERN));
            data.put("alarm.pathpvname", pathPV);
            data.put("loki.label.pvname", pathPV.substring(pathPV.indexOf("//") + 2));
            data.put("loki.label.component", getDataBasedOnPattern(key, COMPONENT_PATTERN));
            data.put("loki.label.severity", rootNode.get("severity").asText());
            data.put("loki.label.alarmtime", Instant.ofEpochSecond(timeNode.get("seconds").asLong(), timeNode.get("nano").asInt()));
            data.put("alarm.value", rootNode.get("value").asDouble());
            data.put("alarm.current.message", rootNode.get("current_message").asText());
            data.put("alarm.current.severity", rootNode.get("current_severity").asText());

            //Send event bus karaf
            dispatcher.sendEvent(new Event(eventAdminTopic, data));
           

        } catch (Exception e) {
            LOGGER.info(e.getMessage());
        }

    }

   

    //Kafka message parameters
    public static String getDataBasedOnPattern(String text, Pattern pattern) {
        if (text == null) {
            return null;
        }

        Matcher matcher = pattern.matcher(text);

        return matcher.find() ? matcher.group(1) : null;
    }

    public static String getPathPV(String keyText) {
        if (keyText == null) {
            return null;
        }

        int indexEndProtocol = keyText.indexOf(ESCAPED_PROTOCOL_SEPARATOR);
        if (indexEndProtocol == -1) {
            indexEndProtocol = keyText.indexOf(PROTOCOL_SEPARATOR);
        }

        if (indexEndProtocol != -1) {
            int indexLastSlash = keyText.lastIndexOf("/", indexEndProtocol);

            if (indexLastSlash != -1) {
                return keyText.substring(indexLastSlash + 1).replace("\\/\\/", "//");
            }
        }
        return null;
    }
}
