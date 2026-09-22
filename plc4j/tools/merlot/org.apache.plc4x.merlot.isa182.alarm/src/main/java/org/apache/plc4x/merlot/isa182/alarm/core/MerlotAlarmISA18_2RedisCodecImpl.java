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
package org.apache.plc4x.merlot.isa182.alarm.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.codec.RedisCodec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import org.apache.plc4x.merlot.isa182.alarm.model.MerlotAlarmISA18_2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Custom message serializer/deserializer for a Redis server
 */
public class MerlotAlarmISA18_2RedisCodecImpl implements RedisCodec<String, MerlotAlarmISA18_2> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MerlotAlarmISA18_2RedisCodecImpl.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String decodeKey(ByteBuffer bytes) {
        return StandardCharsets.UTF_8.decode(bytes).toString();
    }

    @Override
    public MerlotAlarmISA18_2 decodeValue(ByteBuffer bytes) {
        try {
            ByteBuffer duplicate = bytes.duplicate();
            byte[] array = new byte[duplicate.remaining()];
            duplicate.get(array);

            String json = new String(array, StandardCharsets.UTF_8);

            if (json.startsWith("\"") && json.endsWith("\"")) {
                json = objectMapper.readValue(json, String.class);
            }

            return objectMapper.readValue(json, MerlotAlarmISA18_2.class);
        } catch (Exception e) {
            LOGGER.info("Error decoding alarm: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public ByteBuffer encodeKey(String key) {
        return StandardCharsets.UTF_8.encode(key);
    }

    @Override
    public ByteBuffer encodeValue(MerlotAlarmISA18_2 value) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(value);
            return ByteBuffer.wrap(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return ByteBuffer.allocate(0);
        }
    }

}
