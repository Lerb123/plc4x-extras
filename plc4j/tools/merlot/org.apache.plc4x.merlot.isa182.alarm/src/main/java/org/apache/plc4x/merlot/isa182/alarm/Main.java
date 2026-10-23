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
package org.apache.plc4x.merlot.isa182.alarm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.RedisCodec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 *
 * @author luis
 */
public class Main {

    static class Persona {

        String name;
        String lastName;
        int age;

        public Persona(String name, String lastName, int age) {
            this.name = name;
            this.lastName = lastName;
            this.age = age;
        }

        public Persona() {
        }

        public String getName() {
            return name;
        }

        public String getLastName() {
            return lastName;
        }

        public int getAge() {
            return age;
        }
    }

    public static class PersonaRedisCodec implements RedisCodec<String, Persona> {

        private final ObjectMapper objectMapper = new ObjectMapper();

// Codificar la Clave (String)
        @Override
        public ByteBuffer encodeKey(String key) {
            return StandardCharsets.UTF_8.encode(key);
        }

// Decodificar la Clave (String) 
        @Override
        public String decodeKey(ByteBuffer bytes) {
            return StandardCharsets.UTF_8.decode(bytes).toString();
        }

// Codificar el Valor (Objeto Persona -&gt; JSON ByteBuffer) 
        @Override
        public ByteBuffer encodeValue(Persona value) {
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(value);
                return ByteBuffer.wrap(bytes);
            } catch (Exception e) {
                e.printStackTrace();
                return ByteBuffer.allocate(0);
            }
        }
// Decodificar el Valor (JSON ByteBuffer -&gt; Objeto Persona) 

        @Override
        public Persona decodeValue(ByteBuffer bytes) {
            try {
                // Duplicamos el ByteBuffer para asegurar lectura limpia desde su posición actual
                ByteBuffer duplicate = bytes.duplicate();
                byte[] array = new byte[duplicate.remaining()];
                duplicate.get(array);

                String json = new String(array, StandardCharsets.UTF_8);

                // Si el valor guardado previamente en Redis tiene comillas de más ("{...}"), lo limpiamos
                if (json.startsWith("\"") && json.endsWith("\"")) {
                    json = objectMapper.readValue(json, String.class);
                }

                return objectMapper.readValue(json, Persona.class);
            } catch (Exception e) {
                // Imprime el error exacto en la consola si algo falla en la deserialización
                System.err.println("Error decodificando Persona: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }
    }

}
