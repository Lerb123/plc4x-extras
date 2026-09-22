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
package org.apache.plc4x.merlot.api;

import java.util.Optional;

/*
* Objects that implement this interface are responsible for building 
* specific PlcModels. Each equipment model will have a specific structure. 
* For example, Modbus vs S7 vs Rockwell.
*/
public interface PlcModelFactory {
    
    /*
    * @param deviceCategory category of the driver to be instantiated, 
    *        for example s7, s7-light or modbus
    * @param deviceName Technological name of the device, 
    *        generally according to IEC.
    * @return PlcModel according to the specified services.
    */
    public Optional<PlcModel> createPlcModel(String deviceCategory, String deviceName);
    
}
