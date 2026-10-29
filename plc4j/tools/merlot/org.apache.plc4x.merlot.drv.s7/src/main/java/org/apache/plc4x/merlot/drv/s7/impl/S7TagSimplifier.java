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
package org.apache.plc4x.merlot.drv.s7.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.plc4x.merlot.api.TagSimplifier;


public class S7TagSimplifier implements TagSimplifier {

   
    private static final int DEFAULT_MAX_GAP = 15;
 
    private static final Pattern REGEX
            = Pattern.compile("^%?DB(\\d+)\\.DB([XBWD])(\\d+)(?:\\.(\\d+))?$", Pattern.CASE_INSENSITIVE);
 
    // Tag ya parseado.
    // bit   = -1 si el tipo no es DBX.
    // index = posición del tag en la lista que recibe simplify() (-1 si no aplica).
    private record Tag(int dbNum, int startByte, int endByte, int bit, String raw, int index) {}
 
    @Override
    public Offset offsetBase(String tag) {
        Tag t = parse(tag, -1);
        return t == null ? null : new Offset(t.startByte(), t.bit());
    }
 
    /*
    1.
    */
    @Override
    public List<ItemBlock> simplify(List<String> tags, Integer maxGapBytes) {
        int maxGap = (maxGapBytes == null || maxGapBytes == 0) ? DEFAULT_MAX_GAP : maxGapBytes;
 
        // 1. Parsear y agrupar por DB (TreeMap: orden numérico). Cada tag se parsea una sola vez
        //    y recuerda en qué posición de la lista de entrada estaba.
        Map<Integer, List<Tag>> porDb = new TreeMap<>();
        for (int idx = 0; idx < tags.size(); idx++) {
            Tag t = parse(tags.get(idx), idx);
            if (t != null) {
                porDb.computeIfAbsent(t.dbNum(), k -> new ArrayList<>()).add(t);
            }
        }
 
        // 2. Dentro de cada DB: ordenar por byte inicial y cortar cuando la separación supera maxGap
        List<ItemBlock> resultado = new ArrayList<>();
 
        for (Map.Entry<Integer, List<Tag>> entry : porDb.entrySet()) {
            List<Tag> group = entry.getValue();
            //Se ordenan los tags
            group.sort(Comparator.comparingInt(Tag::startByte));
 
            List<Tag> bloque = new ArrayList<>();
            int maxEnd = Integer.MIN_VALUE;
 
            for (Tag t : group) {
                if (!bloque.isEmpty() && t.startByte() - maxEnd - 1 > maxGap) {
                    resultado.add(constructItemBlock(entry.getKey(), bloque));
                    bloque = new ArrayList<>();
                    maxEnd = Integer.MIN_VALUE;
                }
                bloque.add(t);
                maxEnd = Math.max(maxEnd, t.endByte());
            }
 
            if (!bloque.isEmpty()) {
                resultado.add(constructItemBlock(entry.getKey(), bloque));
            }
        }
 
        return resultado;
    }
 
    /*
    2.
    */
    // Los tags llegan ordenados por byte inicial.
    // NO se eliminan duplicados: si dos PV usan el mismo tag, cada uno conserva su entrada,
    // su offset y su índice de origen.
    private static ItemBlock constructItemBlock(int dbNum, List<Tag> tags) {
        int minByte = tags.get(0).startByte();
        int maxByte = tags.stream().mapToInt(Tag::endByte).max().getAsInt();
        int totalBytes = maxByte - minByte + 1;
 
        List<String> originales = new ArrayList<>(tags.size());
        List<Offset> offsets = new ArrayList<>(tags.size());
        List<Integer> indices = new ArrayList<>(tags.size());

        for (Tag t : tags) {
            originales.add(t.raw());
            // Solo se desplaza el byte; el bit no cambia
            offsets.add(new Offset(t.startByte() - minByte, t.bit()));
            indices.add(t.index());
        }
 
        String simplificado = "%DB" + dbNum + ".DBB" + minByte + "[0.." + (totalBytes - 1) + "]:BYTE";
 
        return new ItemBlock("DB" + dbNum, minByte, totalBytes, simplificado,
                List.copyOf(originales), List.copyOf(offsets), List.copyOf(indices));
    }
 
    // null si el texto no es un tag Siemens válido
    private static Tag parse(String original, int index) {
        if (original == null) {
            return null;
        }
        String raw = original.trim();
        if (raw.isEmpty()) {
            return null;
        }
 
        Matcher m = REGEX.matcher(raw);
        if (!m.matches()) {
            return null;
        }
 
        try {
            int dbNum = Integer.parseInt(m.group(1));
            String type = m.group(2).toUpperCase();
            int byteOffset = Integer.parseInt(m.group(3));
 
            int length = switch (type) {
                case "W" -> 2;
                case "D" -> 4;
                default -> 1;
            };
 
            int bit = "X".equals(type)
                    ? (m.group(4) != null ? Integer.parseInt(m.group(4)) : 0)
                    : -1;
 
            return new Tag(dbNum, byteOffset, byteOffset + length - 1, bit, raw, index);
        } catch (NumberFormatException ex) {
            return null;   // números fuera de rango
        }
    }
 
    // Prueba rápida
    public static void main(String[] args) {
        S7TagSimplifier s = new S7TagSimplifier();
        
        //PASO 1. Entregar la lista de tags
        List<String> tags = List.of(
                "%DB1.DBD14", "%DB1.DBW8", "DB1.DBX40.0", "%DB1.DBX45.5", "%DB1.DBW42", "%DB2.DBB5", "%DB2.DBB0");
        List<String> pv = List.of(
                "pv1", "pv8", "pv3", "pv4", "pv9", "pv5", "pv10");
 
        System.out.println("Tags a simplificar: " + tags + "\n");
 
        //PASO 2. El algoritmo te devuelve los bloques
        for (ItemBlock b : s.simplify(tags, 15)) {
            System.out.println("Tag simplificado: " + b.tagSimplificado()
                    + "   (inicia en el byte " + b.startByte() + ")");
            System.out.printf("  %-14s %-13s %-13s%n", "Tag", "Offset base", "Offset nuevo");
 
            for (int i = 0; i < b.tagsOriginales().size(); i++) {
                String tag = b.tagsOriginales().get(i);
                // Posición real del tag en la lista de entrada (y por tanto en pv)
                int src = b.sourceIndexes().get(i);
                System.out.println(String.format("Tag: %s, OffsetBase: %s, OffsetNuevo: %s, PV: %s", tag, s.offsetBase(tag), b.offsetRelativo(i), pv.get(src) ));
            }
            System.out.println();
        }
    }
}