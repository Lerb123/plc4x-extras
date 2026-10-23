/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.plc4x.merlot.isa182.alarm.appender;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.plc4x.merlot.isa182.alarm.api.MerlotAlarmISA18_2Appender;
import org.apache.plc4x.merlot.isa182.alarm.core.MerlotAlarmISA18_2RedisCodecImpl;
import org.apache.plc4x.merlot.isa182.alarm.model.MerlotAlarmISA18_2;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlotAlarmISA18_2RegisterRuntimeAppender implements MerlotAlarmISA18_2Appender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MerlotAlarmISA18_2RegisterRuntimeAppender.class);
    private final static String MERLOT_REGISTER_RUNTIME_EVENT_TOPIC = "merlot/isa18.2/runtime";
    private final static String REDIS_SERVER_URL = "redis.server.url";
    private final Map<String, Object> alarmEventProperties = new HashMap();
    private RedisClient client;
    private String urlConnection;
    private StatefulRedisConnection<String, MerlotAlarmISA18_2> connection;
    private RedisAsyncCommands<String, MerlotAlarmISA18_2> asyncCommands;
    
    @Override
    public void init() {
        //TODO: 
        
         client = RedisClient.create(urlConnection);
         connection = client.connect(new MerlotAlarmISA18_2RedisCodecImpl());
         asyncCommands = connection.async();
    }

    @Override
    public void save(MerlotAlarmISA18_2 alarm) {
        //TODO
    }

    @Override
    public void handleEvent(Event event) {
        //TODO

        LOGGER.info("Processing alarm event, sending to runtime register");

        String topic = event.getTopic();

        if (topic.equalsIgnoreCase(MERLOT_REGISTER_RUNTIME_EVENT_TOPIC)) {

            //Get properties alarm names
            for (String pn : event.getPropertyNames()) {
                alarmEventProperties.put(pn, event.getProperty(pn));
            }
            
            sendAlarmRuntimeRegister(alarmEventProperties);
            alarmEventProperties.clear();
        }
    }

    @Override
    public void destroy() {
       // asyncCommands.
        //client.shutdownAsync();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties.isEmpty() || properties == null) {
            return;
        }

        destroy();
        //Get url: Note: url = redis://192.168.0.246:6379
        //TODO: de igual forma se debe poder ingresar con usuario y contrasena
        String url = (String) properties.get(REDIS_SERVER_URL);
        if (url != null) {
            this.urlConnection = url.trim();
        }
        
        init();

    }

    private void sendAlarmRuntimeRegister(Map<String, Object> alarmProperties) {
        asyncCommands.set(UUID.randomUUID().toString(), new MerlotAlarmISA18_2(alarmProperties))
            .thenAccept(alarm -> {
                if (alarm != null) {
                    LOGGER.info("Saved in Redis");
                } else {
                    LOGGER.info("No saved in Redis");
                }
            })
            .exceptionally(ex -> {
                LOGGER.info(ex.getMessage());
                return null;
            });
    }

}
