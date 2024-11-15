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

import java.util.logging.Level;
import org.apache.plc4x.merlot.decanter.api.MerlotAppender;
import org.apache.plc4x.merlot.scheduler.api.JobContext;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MerlotMqttAppenderImpl  implements MerlotAppender {
    private static final Logger LOGGER = LoggerFactory.getLogger(MerlotMqttAppenderImpl.class); 
    
    private final String strServerUri;
    private final String strClientId;
    private final String strTopic;
    private final String strUserName;
    private final String strPassword;
    private final String strEventTopic;
    private final String strMarshallerTarget;   
    private final String watchdogTime;    

    
    private MqttClient client;
    private MqttMessage message;
    
    MerlotMqttAppenderImpl(Builder build) {
        this.strServerUri = build.strServerUri;
        this.strClientId = build.strClientId;
        this.strTopic = build.strTopic;
        this.strUserName = build.strUserName;
        this.strPassword = build.strPassword;
        this.strEventTopic = build.strEventTopic; 
        this.strMarshallerTarget = build.strMarshallerTarget;           
        this.watchdogTime = build.watchdogTime;
    }
    
    
    @Override
    public void init() {
        try {            
            message = new MqttMessage();
            client = new MqttClient(strServerUri, strClientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(strUserName);
            options.setPassword(strPassword.toCharArray());
            client.connect(options);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
    }

    @Override
    public void destroy() {
        if (null != client)
            try {
                client.close();
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
    }    
        
    @Override
    public void handleEvent(Event event) {
        System.out.println("Evento: " + event.toString());
    }

    @Override
    public void execute(JobContext context) {
        System.out.println("EXECUTE");
    }


    
    
   public static class Builder {
       private String strServerUri;
       private String strClientId;
       private String strTopic;
       private String strUserName;
       private String strPassword;
       private String strEventTopic;
       private String strMarshallerTarget;       
       private String watchdogTime;

       public Builder ServerUri(String strServerUri){
           this.strServerUri = strServerUri;
           return this;
       }
       
       public Builder ClientId(String strClientId){
           this.strServerUri = strClientId;
           return this;
       }       
            
       public Builder Topic(String strTopic){
           this.strTopic = strTopic;
           return this;
       }   
       
       public Builder UserName(String strUserName){
           this.strUserName = strUserName;
           return this;
       } 
       
       public Builder Password(String strPassword){
           this.strPassword = strPassword;
           return this;
       }    
       
       public Builder EventTopic(String strEventTopic){
           this.strEventTopic = strEventTopic;
           return this;
       }   
       
       public Builder WatchDogTime(String watchdogTime){
           this.watchdogTime =  watchdogTime;
           return this;
       } 

       public Builder MarshallerTarget(String strMarshallerTarget){
        this.strMarshallerTarget = strMarshallerTarget;
        return this;
       }
       
       public MerlotMqttAppenderImpl build() {
           return new MerlotMqttAppenderImpl (this);
       } 
       
       
   } 
    
}
