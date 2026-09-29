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
package org.apache.plc4x.merlot.isa182.alarm.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public class MerlotAlarmISA18_2 {
     public static final String ALARM_MODEL_VERSION = "1.0.0";
     
     // --- 1\. Identificación básica y Agrupación --- 
     private String tagName; // Nombre único de la etiqueta o punto (Tag / Point) [4, 5]
     private String alarmDescription; // Descripción corta de la alarma o variable de proceso [6, 7] 
     private AlarmType alarmType; // Tipo de alarma (ej. High, Low, Deviation) [8-10] 
     private String alarmGroup; // Grupo o área del proceso a la que pertenece [6, 11] 
     
    // --- 2\. Atributos de Diseño Básico y Configuración (Clause 10.5) --- 
     private double alarmSetpoint; // Límite o umbral de disparo [6, 9, 12] 
     private AlarmPriority alarmPriority; // Nivel de prioridad o importancia operacional [6, 9, 13, 14] 
     private List <String> alarmClasses;// Clases a las que pertenece (ej. Seguridad, Ambiental) [9, 13, 15]
     private double deadband; // Histéresis para retorno a condición normal sin fluctuar [13, 16] 
     private double onDelaySeconds; // Temporizador de retardo a la activación (filtro de picos) [17] 
     private double offDelaySeconds; // Temporizador de retardo a la desactivación (evita chattering) [17] 
     private String alarmMessage; // Mensaje formateado a desplegar en la HMI [6, 18]
     
     // --- 3\. Atributos de Racionalización y Guía del Operador (Clause 9.2) --- 
     private String operatorAction; // Acción correctiva requerida por el operador [9] 
     private String consequenceOfInaction; // Consecuencia directa de no actuar a tiempo [9] 
     private double allowableResponseTime; // Tiempo máximo permitido para responder antes de la consecuencia [9, 19, 20] 
     private String probableCause; // Causa probable de la condición anómala [21] 
     private String setpointRationale; // Base de diseño / justificación técnica del umbral [21] 
     private String identificationMethod; // Método de identificación (HAZOP, LOPA, PHA, etc.) [21, 22]
     
     // --- 4\. Estado Dinámico y Datos de Tiempo Real (Clause 5.3 &amp; 11.2) --- 
     private AlarmState currentState; // Estado en el ciclo de vida según la matriz ISA-18.2 [23, 24] 
     private boolean active; // Indica si la condición de proceso es anómala [24, 25] 
     private boolean acknowledged; // Indica si la alarma fue reconocida por el operador [24, 25] 
     private SuppressionState suppressionStatus; // Estado de supresión autorizada (Shelved, DSUPR, OOSRV) [26-28] 
     private Instant timestamp; // Fecha y hora del último cambio de estado [29] 
     private double currentProcessValue; // Valor actual de la variable en el momento de la alarma [7, 29]
    
    
    //Getters
    //Setters
    //ToString

    public MerlotAlarmISA18_2(Map<String, Object> properties) {
        
    }
     
     
     
     public enum AlarmPriority { 
         LOW,
         MEDIUM,
         HIGH,
         HIGHEST // Priorización según impacto y tiempo de respuesta [30, 31] 
     }
     
     public enum AlarmType { 
         ABSOLUTE_HIGH,
         ABSOLUTE_LOW, 
         HIGH_HIGH, 
         LOW_LOW,
         DEVIATION,
         RATE_OF_CHANGE,
         BAD_MEASUREMENT,
         DISCREPANCY 
     }
     
     public enum AlarmState { 
         NORMAL, // Estado Normal (NORM) [24, 34]
         UNACKNOWLEDGED, // Alarma Activa No Reconocida (UNACK) [24, 34] 
         ACKNOWLEDGED, // Alarma Activa Reconocida (ACKED) [24, 27]
         RETURN_TO_NORMAL_UNACKNOWLEDGED // Retornó a Normal No Reconocido (RTNUN) [24, 27] 
     }
     
     public enum SuppressionState { 
         UNSUPPRESSED, // Sin supresión (Visible al operador) [23, 35] 
         SHELVED, // Supresión temporal manual por el operador (SHLVD) [24, 27] 
         SUPPRESSED_BY_DESIGN, // Supresión lógica automática por estado de planta (DSUPR) [24, 28] 
         OUT_OF_SERVICE // Fuera de servicio por mantenimiento/reparación (OOSRV) [24, 28] 
     }
     
}

