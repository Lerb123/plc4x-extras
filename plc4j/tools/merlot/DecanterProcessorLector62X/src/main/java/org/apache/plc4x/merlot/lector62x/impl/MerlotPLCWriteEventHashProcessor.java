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

import org.apache.plc4x.merlot.lector62x.api.MerlotDecanterHashUtility;
import org.osgi.service.event.Event;

public class MerlotPLCWriteEventHashProcessor implements MerlotDecanterHashUtility {

    private int[] bufferClass;

    public MerlotPLCWriteEventHashProcessor() {
        //TODO: Pedir al blueprint el GPclient (wrapper)
        bufferClass = new int[4];
    }

    public void init() {
        //TODO: Arranca monitoreo del pva del piston
    }

    public void destroy() {
        this.bufferClass = null;
    }

    //Separa el numero en 4 digitos y lo guarda en el arreglo ()
    private void setValuesFromEventDestiny(String officeDestiny) {
        bufferClass[0] = (Character.getNumericValue(officeDestiny.charAt(0)));
        bufferClass[1] = (Character.getNumericValue(officeDestiny.charAt(1)));
        bufferClass[2] = (Character.getNumericValue(officeDestiny.charAt(2)));
        bufferClass[3] = (Character.getNumericValue(officeDestiny.charAt(3)));
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
         * Ya existe el Gpclient leyendo/escribiendo el pva del piston
         * Ejecutar la escritura al pva
         */
    }

    @Override
    public void handleEvent(Event event) {
        if (event.equals(MERLOT_SERVICE_HASH_TOPIC)) {

            //TODO: Extraer los datos del evento
            //Propiedad del evento: officeDestiny
            //1. Separar el numero en digitos
            try {
                setValuesFromEventDestiny((String) event.getProperty(DESTINY_OFFICE));
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            //2. Aplicar Hash al arreglo de digitos y mandar a escribir con gpclient
            writePV(applyHash());
        }
    }

    public int[] getBufferClass() {
        return bufferClass;
    }

    public static void main(String[] args) {
        MerlotPLCWriteEventHashProcessor m = new MerlotPLCWriteEventHashProcessor();
        String n = "1234";
        m.setValuesFromEventDestiny(n);
        System.out.println("Hash: " + m.applyHash());

        for (int bufferClas : m.getBufferClass()) {
            System.out.println("N: " + bufferClas);
        }
    }
}
