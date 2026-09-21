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

import java.time.Duration;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.plc4x.merlot.kafka.api.MerlotDecanterCollector;
import org.osgi.service.event.EventConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlotKafkaDecanterCollectorImpl implements MerlotDecanterCollector, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MerlotKafkaDecanterCollectorImpl.class);

    private String topic;
    private String eventAdminTopic;
    private volatile boolean consuming = false;
    private String messageType;

    private Dictionary<String, Object> properties;
    private KafkaConsumer<String, String> consumer;


    private ExecutorService executor;
    private final MerlotKafkaDecanterProcessorImpl alarmProcessor;

    public MerlotKafkaDecanterCollectorImpl(MerlotKafkaDecanterProcessorImpl alarmProcessor) {
        this.alarmProcessor = alarmProcessor;
    }

    @Override
    public void init() {
        consuming = true;
        this.executor = Executors.newSingleThreadExecutor();
        this.executor.execute(this);
    }

    @Override
    public void destroy() {
        consuming = false;
        if (consumer != null) {
            consumer.wakeup();
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void activate(String pid, Dictionary<String, Object> properties) {
        this.properties = properties;

        topic = getValue(properties, "topic", "decanter");
        eventAdminTopic = getValue(properties, EventConstants.EVENT_TOPIC, "decanter/collect/kafka/decanter");
        messageType = getValue(properties, "message.type", "text");

        //Config properties kafka consumer
        Properties config = new Properties();

        String bootstrapServers = getValue(properties, "bootstrap.servers", "localhost:9092");
        config.put("bootstrap.servers", bootstrapServers);

        String groupId = getValue(properties, "group.id", "decanter");
        config.put("group.id", groupId);

        String enableAutoCommit = getValue(properties, "enable.auto.commit", "true");
        config.put("enable.auto.commit", enableAutoCommit);

        String autoCommitIntervalMs = getValue(properties, "auto.commit.interval.ms", "1000");
        config.put("auto.commit.interval.ms", autoCommitIntervalMs);

        String sessionTimeoutMs = getValue(properties, "session.timeout.ms", "10000");
        config.put("session.timeout.ms", sessionTimeoutMs);

        String keyDeserializer = getValue(properties, "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("key.deserializer", keyDeserializer);

        String valueDeserializer = getValue(properties, "value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        config.put("value.deserializer", valueDeserializer);

        String securityProtocol = getValue(properties, "security.protocol", null);
        if (securityProtocol != null) {
            config.put("security.protocol", securityProtocol);
        }

        String sslTruststoreLocation = getValue(properties, "ssl.truststore.location", null);
        if (sslTruststoreLocation != null) {
            config.put("ssl.truststore.location", sslTruststoreLocation);
        }

        String sslTruststorePassword = getValue(properties, "ssl.truststore.password", null);
        if (sslTruststorePassword != null) {
            config.put("ssl.truststore.password", sslTruststorePassword);
        }

        String sslKeystoreLocation = getValue(properties, "ssl.keystore.location", null);
        if (sslKeystoreLocation != null) {
            config.put("ssl.keystore.location", sslKeystoreLocation);
        }

        String sslKeystorePassword = getValue(properties, "ssl.keystore.password", null);
        if (sslKeystorePassword != null) {
            config.put("ssl.keystore.password", sslKeystorePassword);
        }

        String sslKeyPassword = getValue(properties, "ssl.key.password", null);
        if (sslKeyPassword != null) {
            config.put("ssl.key.password", sslKeyPassword);
        }

        String sslProvider = getValue(properties, "ssl.provider", null);
        if (sslProvider != null) {
            config.put("ssl.provider", sslProvider);
        }

        String sslCipherSuites = getValue(properties, "ssl.cipher.suites", null);
        if (sslCipherSuites != null) {
            config.put("ssl.cipher.suites", sslCipherSuites);
        }

        String sslEnabledProtocols = getValue(properties, "ssl.enabled.protocols", null);
        if (sslEnabledProtocols != null) {
            config.put("ssl.enabled.protocols", sslEnabledProtocols);
        }

        String sslTruststoreType = getValue(properties, "ssl.truststore.type", null);
        if (sslTruststoreType != null) {
            config.put("ssl.truststore.type", sslTruststoreType);
        }

        String sslKeystoreType = getValue(properties, "ssl.keystore.type", null);
        if (sslKeystoreType != null) {
            config.put("ssl.keystore.type", sslKeystoreType);
        }

        ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(null);
            consumer = new KafkaConsumer<String, String>(config);
            String[] topics = topic.split(",");
            for (int i = 0; i < topics.length; i++) {
                topics[i] = topics[i].replaceAll("\\s+", "");
            }
            consumer.subscribe(Arrays.asList(topics));
        } finally {
            Thread.currentThread().setContextClassLoader(originClassLoader);
        }
    }

    @Override
    public void run() {
        try {
            while (consuming) {
                try {
                    consume();
                } catch (WakeupException e) {
                } catch (Exception e) {
                    LOGGER.info(e.getMessage(), e);
                }
            }
        } finally {
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (Exception e) {
                    LOGGER.info("Error closing Kafka consumer", e);
                }
            }
        }
    }

    private void consume() {
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

        if (records.isEmpty()) {
            return;
        }

        for (ConsumerRecord<String, String> record : records) {

            if (!consuming) {
                return;
            }
            try {
                this.alarmProcessor.processRecord(eventAdminTopic, record);
            } catch (Exception e) {
                LOGGER.warn("Registro descartado (key={})", record.key(), e);
            }

        }
    }

    //Initial parameters
    private String getValue(Dictionary<String, Object> config, String key, String defaultValue) {
        String value = (String) config.get(key);
        return (value != null) ? value : defaultValue;
    }

}
