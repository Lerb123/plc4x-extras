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
package org.apache.plc4x.merlot.decanter.impl;


import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.plc4x.merlot.decanter.api.BrokerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class BrokerServiceImpl implements BrokerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerServiceImpl.class);    
    private EmbeddedActiveMQ server= null;
    
    @Override
    public void init() {
        System.out.println("Iniciando");
        Configuration config = new ConfigurationImpl(); 
        try { 
            config.addAcceptorConfiguration("in-vm", "vm://0");
            config.addAcceptorConfiguration("mqtt", "tcp://127.0.0.1:1883?protocols=MQTT;closeMqttConnectionOnPublishAuthorizationFailure=false");        

            server = new EmbeddedActiveMQ();
            server.setConfiguration(config);
       
            server.start();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        
    }

    @Override
    public void destroy() {
        try {
            server.stop();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
    }
    
}
