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

import org.apache.plc4x.merlot.isa182.alarm.api.MerlotAlarmISA18_2Appender;
import org.apache.plc4x.merlot.isa182.alarm.model.MerlotAlarmISA18_2;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlotAlarmISA18_2RegisterRuntimeAppender implements MerlotAlarmISA18_2Appender {

    private static final Logger LOGGER = LoggerFactory.getLogger(MerlotAlarmISA18_2RegisterRuntimeAppender.class);

    @Override
    public void save(MerlotAlarmISA18_2 alarm) {
        //TODO
    }

    @Override
    public void handleEvent(Event event) {
        //TODO
    }

}
