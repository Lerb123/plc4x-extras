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
<<<<<<<< HEAD:plc4j/tools/merlot/org.apache.plc4x.merlot.isa182.alarm/src/main/java/org/apache/plc4x/merlot/isa182/alarm/controller/MerlotAlarmISA18_2WebSocketAdapter.java
package org.apache.plc4x.merlot.isa182.alarm.controller;

public class MerlotAlarmISA18_2WebSocketAdapter {
    
}
========
package org.apache.plc4x.nifi.subscription;

public enum Plc4xSubscriptionType {
    CHANGE, // of state (Event is sent as soon as a value changes)
    CYCLIC, //(The Event is sent in regular cyclic intervals)
    EVENT //(The Event is usually explicitly sent form the PLC as a signal)
}
>>>>>>>> f4dff1e (Fix: headers rat (#726)):plc4j/integrations/apache-nifi/nifi-2/nifi-2-plc4x-processors/src/main/java/org/apache/plc4x/nifi/subscription/Plc4xSubscriptionType.java
