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

import org.apache.kafka.clients.consumer.ConsumerConfig;
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

    private KafkaConsumer<String, String> consumer;
    private ExecutorService executor;
    private final MerlotKafkaDecanterProcessorImpl alarmProcessor;

    // Control de Inactividad
    private int emptyPollCount = 0;
    // Si hace 10 polls seguidos sin recibir nada (10 segundos), detiene el proceso
    private static final int MAX_EMPTY_POLLS = 10; 

    public MerlotKafkaDecanterCollectorImpl(MerlotKafkaDecanterProcessorImpl alarmProcessor) {
        this.alarmProcessor = alarmProcessor;
    }

    @Override
    public void init() {
        consuming = true;
        this.emptyPollCount = 0;
        this.executor = Executors.newSingleThreadExecutor(r -> new Thread(r, "KafkaDecanterCollector-" + topic));
        this.executor.execute(this);
    }

    @Override
    public void destroy() {
        consuming = false;
        if (consumer != null) {
            try {
                consumer.wakeup(); // Despierta el poll inmediatamente
            } catch (Exception e) {
                LOGGER.warn("Error en wakeup del consumidor", e);
            }
        }
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void activate(String pid, Dictionary<String, Object> properties) {
        if (this.consumer != null) {
            closeConsumer();
        }

        topic = getValue(properties, "topic", "decanter");
        eventAdminTopic = getValue(properties, EventConstants.EVENT_TOPIC, "decanter/collect/kafka/decanter");

        Properties config = new Properties();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, getValue(properties, "bootstrap.servers", "localhost:9092"));
        config.put(ConsumerConfig.GROUP_ID_CONFIG, getValue(properties, "group.id", "decanter"));
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, getValue(properties, "enable.auto.commit", "true"));
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, getValue(properties, "key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, getValue(properties, "value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer"));

        //TODO: Security features are missing (ADD)
        
        //Note: Zero retention and short timeouts to prevent RAM from becoming fragmented
        config.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        config.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "5000");
        config.put(ConsumerConfig.RECEIVE_BUFFER_CONFIG, "32768");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "50");

        ClassLoader originClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(KafkaConsumer.class.getClassLoader());
            consumer = new KafkaConsumer<>(config);
            consumer.subscribe(Arrays.asList(topic.split(",")));
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
                    LOGGER.info("Kafka consumer detained by explicit order.");
                    break;
                } catch (Exception e) {
                    LOGGER.error("Critical error in Kafka. Shutting down the collector completely to avoid consuming memory.", e);
                    break;
                }
            }
        } finally {
            consuming = false;
            closeConsumer();
            LOGGER.info("Kafka collector completely shut down.");
        }
    }

    private void consume() {
        // Poll de 1 segundo
        ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

        if (records.isEmpty()) {
            emptyPollCount++;
            LOGGER.debug("There is no data in Kafka. Attempt {} of {}", emptyPollCount, MAX_EMPTY_POLLS);

            // Note:If there are no messages after X attempts, the entire thread is shut down
            if (emptyPollCount >= MAX_EMPTY_POLLS) {
                LOGGER.warn("Kafka has not sent any data for {} seconds. Stopping the collector completely...", MAX_EMPTY_POLLS);
                consuming = false;
            }
            return;
        }

        // Note: If data is received, the inactivity counter is reset
        emptyPollCount = 0;

        for (ConsumerRecord<String, String> record : records) {
            if (!consuming) {
                return;
            }
            try {
                this.alarmProcessor.processRecord(eventAdminTopic, record);
            } catch (Exception e) {
                LOGGER.warn("Error processing record; discarding without pasting: {}", record.key(), e);
            }
        }
    }

    private synchronized void closeConsumer() {
        if (consumer != null) {
            try {
                consumer.unsubscribe();
                consumer.close(Duration.ofSeconds(1));
            } catch (Exception e) {
                LOGGER.warn("Error closing the KafkaConsumer client", e);
            } finally {
                consumer = null; // Liberar referencia para el GC de Java
            }
        }
    }

    private String getValue(Dictionary<String, Object> config, String key, String defaultValue) {
        if (config == null) return defaultValue;
        Object value = config.get(key);
        return (value != null) ? value.toString() : defaultValue;
    }
}