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
import org.apache.kafka.clients.consumer.ConsumerRecord;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlotKafkaDecanterProcessorImplTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MerlotKafkaDecanterProcessorImplTest.class);
    private static final String EVENT_TOPIC = "test";
    private static ConsumerRecord<String, String> record;
    private static MerlotKafkaDecanterProcessorImpl processor;

    @BeforeAll
    public static void setUp() {
        String topic = "topic";
        int partition = 0;
        long offset = 0L;
        String key = "state:/factory/Area 1/Linea 1/Sala 1/Maquina 1/sim:\\/\\/sine";
        String value = "{\"severity\":\"MAJOR\",\"message\":\"LOLO\",\"value\":\"-4.755282581475767\",\"time\":{\"seconds\":1781975416,\"nano\":456498068},\"current_severity\":\"MAJOR\",\"current_message\":\"LOLO\"}";

        record = new ConsumerRecord<>(topic, partition, offset, key, value);
        processor = new MerlotKafkaDecanterProcessorImpl(new EventAdmin() {
            @Override
            public void postEvent(Event event) {
                assertEquals("test", event.getTopic());
                assertEquals("MerlotAlarmCollector", event.getProperty("loki.label.job"));
                assertEquals("MAJOR", event.getProperty("alarm.current.severity"));
                assertEquals(Instant.parse("2026-06-20T17:10:16.456498068Z"), event.getProperty("loki.label.alarmtime"));
                assertEquals("sim://sine", event.getProperty("alarm.pathpvname"));
                assertEquals("LOLO", event.getProperty("alarm.current.message"));
                assertEquals("factory", event.getProperty("loki.label.topicalarm"));
                assertEquals("sine", event.getProperty("loki.label.pvname"));
                assertEquals("Area 1/Linea 1/Sala 1/Maquina 1", event.getProperty("loki.label.component"));
                assertEquals("MAJOR", event.getProperty("loki.label.severity"));
                assertEquals("-4.755282581475767", event.getProperty("alarm.value"));
            }

            @Override
            public void sendEvent(Event event) {
                //Not use
            }
        });
    }

    @Test
    public void processData() {
        processor.processRecord(EVENT_TOPIC, record);
    }
}
