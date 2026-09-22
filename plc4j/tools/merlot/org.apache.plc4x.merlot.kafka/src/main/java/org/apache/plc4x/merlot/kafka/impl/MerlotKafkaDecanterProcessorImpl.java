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
    private static final Pattern SERVERITY_PATTERN = Pattern.compile("\"severity\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VALUE_PATTERN = Pattern.compile("\"value\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CURRENT_MESSAGE_PATTERN = Pattern.compile("\"current_message\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern CURRENT_SEVERITY_PATTERN = Pattern.compile("\"current_severity\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern TIME_PATTERN = Pattern.compile("\"time\"\\s*:\\s*\\{\\s*\"seconds\"\\s*:\\s*(\\d+)\\s*,\\s*\"nano\"\\s*:\\s*(\\d+)\\s*\\}");
    private static final String ESCAPED_PROTOCOL_SEPARATOR = ":\\/\\/";
    private static final String PROTOCOL_SEPARATOR = "://";

    private final EventAdmin dispatcher;

    public MerlotKafkaDecanterProcessorImpl(EventAdmin dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void processRecord(String eventAdminTopic, ConsumerRecord<String, String> record) {

        try {

            Map<String, Object> data = new HashMap<>();
            data.put("loki.label.job", "MerlotAlarmCollector");

            //Data headers
            String key = record.key();

            //Alarm values
            String value = record.value();

            //LOGGER.info("Key: {} Value: {}", key, value);
            String pathPV = getPathPV(key);

            //Loki paramaters
            data.put("loki.label.topicalarm", getDataBasedOnPattern(key, TOPIC_ALARM_PATTERN));
            data.put("alarm.pathpvname", pathPV);
            data.put("loki.label.pvname", pathPV.substring(pathPV.indexOf("//") + 2));
            data.put("loki.label.component", getDataBasedOnPattern(key, COMPONENT_PATTERN));
            data.put("loki.label.severity", getDataBasedOnPattern(value, SERVERITY_PATTERN));
            data.put("loki.label.alarmtime", getAlarmTime(value));
            data.put("alarm.value", getDataBasedOnPattern(value, VALUE_PATTERN));
            data.put("alarm.current.message", getDataBasedOnPattern(value, CURRENT_MESSAGE_PATTERN));
            data.put("alarm.current.severity", getDataBasedOnPattern(value, CURRENT_SEVERITY_PATTERN));

            //Send event bus karaf
            dispatcher.postEvent(new Event(eventAdminTopic, data));

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

    public static Instant getAlarmTime(String valueText) {
        if (valueText == null) {
            return null;
        }
        Matcher m = TIME_PATTERN.matcher(valueText);
        return m.find() ? Instant.ofEpochSecond(Long.parseLong(m.group(1)), Long.parseLong(m.group(2))) : null;
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
