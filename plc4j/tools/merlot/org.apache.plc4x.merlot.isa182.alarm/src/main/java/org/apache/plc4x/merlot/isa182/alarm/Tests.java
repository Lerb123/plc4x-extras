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

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import org.apache.plc4x.merlot.isa182.alarm.Main.Persona;

/**
 *
 * @author luis
 */
public class Tests {

    public static void main(String[] args) throws InterruptedException {

    RedisClient client = RedisClient.create("redis://192.168.0.246:6379");
    StatefulRedisConnection<String, Main.Persona> connection = client.connect(new Main.PersonaRedisCodec());
    RedisAsyncCommands<String, Main.Persona> asyncCommands = connection.async();

    asyncCommands.set("persona:2", new Main.Persona("Luis", "Perez", 35))
            .thenCompose(status -> asyncCommands.get("persona:2"))
            .thenAccept(p -> {
                if (p != null) {
                    System.out.println("Persona: " + p.getName());
                } else {
                    System.out.println("No se encontro la persona");
                }
            })
            .exceptionally(ex -> {
                System.err.println("Error: " + ex.getMessage());
                ex.printStackTrace();
                return null;
            })
            .whenComplete((res, ex) -> {
                // Cerramos los recursos SOLO cuando toda la cadena asíncrona termine
                connection.close();
                client.shutdown();
            });

    // Mantenemos el hilo principal vivo mientras Redis responde
    Thread.sleep(2000);
}
}
