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
package org.apache.plc4x.merlot.lector62x.impl;

import java.net.DatagramSocket;
import java.net.InetAddress;
import org.apache.plc4x.merlot.archiver.api.MerlotGPClient;
import org.apache.plc4x.merlot.lector62x.api.MerlotDecanterHashUtility;
import org.epics.gpclient.GPClientInstance;
import org.osgi.service.event.Event;
import org.slf4j.LoggerFactory;

public class MerlotPLCWriteEventHashProcessorImpl implements MerlotDecanterHashUtility {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MerlotPLCWriteEventHashProcessorImpl.class);
    private int[] bufferClass;
    private final GPClientInstance gpClient;

    public MerlotPLCWriteEventHashProcessorImpl(MerlotGPClient gpMerlotClient) {
        this.gpClient = gpMerlotClient.gpClientFactory("MerlotHashTable");
        bufferClass = new int[4];
    }

    
    public void init() {
        //TODO: Arranca monitoreo del pva del piston
    }

    public void destroy() {
        this.bufferClass = null;
        try {
            this.gpClient.close();
        } catch (Exception ex) {
            LOGGER.info("Close connection GPClient");
        }
    }

    @Override
    public int applyHash() {
        int h = (bufferClass[0] << 24) | (bufferClass[1] << 16) | (bufferClass[2] << 8) | bufferClass[3];   // empaquetar
        h ^= h << 3;     // operación 1
        h ^= h >>> 10;   // operación 2
        h ^= h << 21;    // operación 3
        return h >>> 24; // byte alto -> 0..255
    }

    @Override
    public void writePV(int codeHash) {
        //TODO:
        /**
         * Ya existe el Gpclient leyendo/escribiendo el pva del piston Ejecutar
         * la escritura al pva
         */
    }

    @Override
    public void handleEvent(Event event) {
        
        if (event.equals(MERLOT_LECTOR62X_EVENT_TOPIC)) {
            String payloadEvent = (String) event.getProperty("payload");
            try {
                writePV(searchValueFromEventLector(payloadEvent.split(";")));
            } catch (Exception e) {
               LOGGER.info(e.getMessage());
            }
        }
    }

    public int[] getBufferClass() {
        return bufferClass;
    }

    //Separa el numero en 4 digitos y lo guarda en el arreglo ()
    private int searchValueFromEventLector(String... data) {

        //Deben ser enviados como numero a SQL
        String officeDestiny = (String) data[4];
        String idTypeProcessPLC = data[9];

        //TODO: Disponer del service para buscar el registro que coincida entre oficinadestino y idTipoProcesoPLC
        //TODO: Consulta a la tabla piston enviando ambos paramtros y esperando
        bufferClass[0] = (Character.getNumericValue(officeDestiny.charAt(0)));
        bufferClass[1] = (Character.getNumericValue(officeDestiny.charAt(1)));
        bufferClass[2] = (Character.getNumericValue(officeDestiny.charAt(2)));
        bufferClass[3] = (Character.getNumericValue(officeDestiny.charAt(3)));
        return 0;
    }
    
    /**
     * El finalizador (o mezclador final, conocido como fmix) de MurmurHash3 es 
     * una serie de operaciones bit a bit y multiplicaciones con constantes que 
     * provocan un efecto avalancha. Esto significa que cambiar un solo bit en 
     * la entrada modifica aproximadamente la mitad de los bits en el resultado final.
     * @param x
     * @return 
     */
public static int mix(int x) {
    x ^= x >>> 16;
    x *= 0x85ebca6b;
    x ^= x >>> 13;
    x *= 0xc2b2ae35;
    x ^= x >>> 16;
    return x;
}

public static  int applyHash(int office, int processType) {
    int h = mix(office * 8 + processType);   // 8 tipos por oficina
    return Integer.remainderUnsigned(h, 500);
}
   
}
