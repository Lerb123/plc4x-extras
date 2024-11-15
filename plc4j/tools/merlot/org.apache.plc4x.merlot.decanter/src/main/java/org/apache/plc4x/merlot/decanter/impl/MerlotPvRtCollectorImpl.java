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

import java.time.Instant;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.plc4x.merlot.decanter.api.MerlotCollector;
import org.apache.plc4x.merlot.decanter.core.MerlotDecanterManagedService;
import org.apache.plc4x.merlot.scheduler.api.Job;
import org.apache.plc4x.merlot.scheduler.api.JobContext;
import org.apache.plc4x.merlot.scheduler.api.ScheduleOptions;
import org.apache.plc4x.merlot.scheduler.api.Scheduler;
import org.apache.plc4x.merlot.scheduler.api.SchedulerError;
import org.epics.gpclient.GPClient;
import org.epics.gpclient.PVEventRecorder;
import org.epics.gpclient.PVReader;
import org.epics.vtype.VType;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventProperties;
import org.slf4j.LoggerFactory;


public class MerlotPvRtCollectorImpl implements MerlotCollector, ManagedService {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MerlotPvRtCollectorImpl.class);  
    private static final String HTC_ROUTE = "decanter/collector/rt";
    private static final Pattern GROUP_INDEX_PATTERN =
        Pattern.compile("^G(?<groupIndex>\\d{4})"); 
    private static final Pattern PV_INDEX_PATTERN =
        Pattern.compile("^PV(?<groupIndex>\\d{4})");    
    
    protected static final String GROUP_INDEX = "groupIndex";    
       
    private final Scheduler scheduler;
    private final EventAdmin eventAdmin;
    private final Map<String, SchedulerGroup> groups = new ConcurrentHashMap<>();     
    private final Map<String, MutablePair<SchedulerGroup, PVReader<VType>>> pvs = new ConcurrentHashMap<>();    

    public MerlotPvRtCollectorImpl(Scheduler scheduler, EventAdmin eventAdmin) {
        this.scheduler = scheduler;
        this.eventAdmin = eventAdmin;
    }
    

    @Override
    public void init() {

    }

    @Override
    public void destroy() {

    }

    @Override
    public void stop() {
        groups.forEach((g, o) -> {
            scheduler.unschedule(g);
        });
    }

    @Override
    public void start() {
        groups.forEach((g, o) -> {
            try {
                scheduler.schedule(o, o.getScheduleOptions());
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
            }
        });
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        Matcher matcher;        
        String strObject;
        String strKey;
        String strValue;
        
        stop();
        groups.clear();
        
        if (null == properties) return;      
        Enumeration<String> enumKeys = properties.keys();
        while(enumKeys.hasMoreElements()) {
            strKey = enumKeys.nextElement();
            strValue = (String) properties.get(strKey);
            if ((matcher = GROUP_INDEX_PATTERN.matcher(strKey)).matches()) {
                System.out.println("strKey: " + strKey);                
                System.out.println("strValue: " + strValue);
                addGroup(strKey, strValue);                
            }   
        }
        
        enumKeys = properties.keys();
        while(enumKeys.hasMoreElements()) {
            strKey = enumKeys.nextElement();
            strValue = (String) properties.get(strKey);
            if ((matcher = PV_INDEX_PATTERN.matcher(strKey)).matches()) {
                System.out.println("> strKey: " + strKey);                
                System.out.println("> strValue: " + strValue);
                String[] fields = strValue.split(";");
                System.out.println("Length  : " + fields.length);
                System.out.println("PV      : " + fields[0]);
                System.out.println("Group   : " + fields[1]);
                System.out.println("Change  : " + fields[2]);
                final SchedulerGroup group = groups.get(fields[1]);
                if (null != group) {
                    PVInfo pvInfo = new PVInfo();
                    pvInfo.strPv    = fields[0];
                    pvInfo.strGroup = fields[1]; 
                    pvInfo.delta    = Float.parseFloat(fields[2]) / 100;
                    PVEventRecorder recorder = new PVEventRecorder();
                    PVReader<VType> pvr = GPClient.read(pvInfo.strPv).
                            addListener(recorder).
//                            addReadListener((event, p) -> {
//                            System.out.println(event + " " + p.isConnected() + " " + p.getValue());}).
                            start();
                    pvInfo.pvr = pvr;
                    pvInfo.lastValue = null;
                    group.addPvReader(strKey, pvInfo);
                }
            };     
        }        
        
        
    }
            
    @Override
    public void addGroup(String strGroup, String... args) {
        if (null != args) {
            if (null != args[0]) {
                Integer period = Integer.parseInt(args[0]) * 1000;
                ScheduleOptions schOptions = scheduler.AT(Date.from(Instant.now()), -1, period);
                schOptions.name(strGroup);
                
                SchedulerGroup group = new SchedulerGroup(eventAdmin, schOptions);
                groups.put(strGroup, group);
                
                try {
                    scheduler.schedule(group, schOptions);
                } catch (Exception ex) {
                    LOGGER.error(ex.getMessage());
                } 
                
            }
        }
    }

    @Override
    public void removeGroup(String strGroup) {
        scheduler.unschedule(strGroup);                        
        groups.remove(strGroup);
    }

    @Override
    public void schedulerGroup(String strGroup, int scanTime) {


    }

    @Override
    public void putPvRecord(String strPvName, String... args) {
        if (null != args){
            if (null != args[0]) {
                
            }
        }
    }

    @Override
    public void removePvRecord(String strPvName) {

    }

    private class PVInfo {
        public PVReader<VType> pvr;
        public VType lastValue;
        public String strPv;
        public String strGroup;
        public Float delta;        
    }

    private class SchedulerGroup implements Job {
        private final EventAdmin eventAdmin; 
        private final ScheduleOptions schOptions;
        
        private final Map<String, PVInfo> pvs = new ConcurrentHashMap<>();         
        private Map<String, String>  properties = new Hashtable();
        private VType value;
        
        public SchedulerGroup(EventAdmin eventAdmin, ScheduleOptions schOptions) {
            this.eventAdmin = eventAdmin;
            this.schOptions = schOptions;
        }
        
        @Override
        public void execute(JobContext context) {            
            LOGGER.info("GRUPO EXECUTE RT " + HTC_ROUTE);
            pvs.forEach((s, pv) -> {
                System.out.println("<<EXECUTE>>");
                if ((pv.pvr.isConnected()) && (!pv.pvr.isPaused())) {
                    value = pv.pvr.getValue();
                    System.out.println("Valor: " + pv.pvr.getValue());
                    if ((null == pv.lastValue) || !value.equals(pv.lastValue)) {
                        pv.lastValue = value;
                        
                        properties.clear();
                        properties.put(pv.strPv, value.toString());                        
                        EventProperties eventProps = new EventProperties(properties);                                                    

                        Event decanterEvent = new Event(HTC_ROUTE, properties); 
                        eventAdmin.postEvent(decanterEvent);                        
                                                                        
                    }
                }
            });

        }
        
        public void addPvReader(final String strPVIndex, final PVInfo pvInfo){
            pvs.put(strPVIndex, pvInfo);
        }

        public ScheduleOptions getScheduleOptions(){
            return this.schOptions;
        }
        
    }
    
    
}
