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
package org.apache.plc4x.merlot.decanter.core;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Optional;
import org.apache.plc4x.merlot.decanter.api.MerlotAppender;
import org.apache.plc4x.merlot.decanter.api.MerlotDecanterFactory;
import org.apache.plc4x.merlot.scheduler.api.Job;
import org.apache.plc4x.merlot.scheduler.api.JobContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedServiceFactory;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MerlotDecanterManagedService implements ManagedServiceFactory, Job {
    private static final Logger LOGGER = LoggerFactory.getLogger(MerlotDecanterManagedService.class);
    
    private final BundleContext ctx;

    public MerlotDecanterManagedService(BundleContext ctx) {
        this.ctx = ctx;
    }
        
    @Override    
    public void updated(String pid, Dictionary<String, ?> props) throws ConfigurationException {   
        LOGGER.info("UPDATE 01");
        String strFactory = (String) props.get("factory");
        
        if (null != strFactory) {
            MerlotDecanterFactory factory = getFactory(strFactory);
            if (null != factory) {
                Optional<MerlotAppender> optBundle = factory.createBundle(props);
                if (optBundle.isPresent()) {
                    //Register the decanter service

                    String strEventTopic = (String) props.get("eventtopic");
                    System.out.println("CONFIGURANDO: " + strEventTopic);                    
                    Dictionary<String, String> properties = new Hashtable<>();
                    properties.put(EventConstants.EVENT_TOPIC, strEventTopic);
                    ctx.registerService(new String[]{MerlotAppender.class.getName(), EventHandler.class.getName()} , optBundle.get(), properties);
                    
                } else {
                    LOGGER.info("Bundle for [" + strFactory +"] not present.");                     
                }

            } else {
                LOGGER.info("Factory service [" + strFactory +"] don't found.");                
            }                        
        } else {
            LOGGER.info("Factory string [" + strFactory +"] don't found in config file.");
        }
    }
    
    @Override
    public void execute(JobContext context) {
        LOGGER.info("EXECUTE");
    }

    @Override
    public String getName() {
        LOGGER.info("GET NAME");
        return "Merlot-Decanter";
    }

    @Override
    public void deleted(String pid) {
        LOGGER.info("DELETE");
    }
    
    private MerlotDecanterFactory getFactory(String strFactory){
        try{
            String filterdriver =  "(org.plc4x.merlot.decanter.factory=" + strFactory + ")"; 
            ServiceReference[] refdrvs = ctx.getAllServiceReferences(MerlotDecanterFactory.class.getName(), filterdriver);
            MerlotDecanterFactory  refDev = (MerlotDecanterFactory) ctx.getService(refdrvs[0]);
            if (refDev == null) LOGGER.info("Device [" + strFactory + "] don't found");
            return refDev;            
        } catch (Exception ex){
            LOGGER.error("getDevice: " + ex.toString());
        }
        return null;
    }     
    
    
}
