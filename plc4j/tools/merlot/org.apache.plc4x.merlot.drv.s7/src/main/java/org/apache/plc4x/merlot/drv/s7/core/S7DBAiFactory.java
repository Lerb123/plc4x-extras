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
package org.apache.plc4x.merlot.drv.s7.core;

import io.netty.buffer.Unpooled;
import org.apache.plc4x.merlot.api.PlcItem;
import org.apache.plc4x.merlot.api.PlcItemListener;
import org.apache.plc4x.merlot.db.api.DBRecord;
import org.apache.plc4x.merlot.db.core.DBBaseFactory;
import org.epics.nt.NTScalar;
import org.epics.nt.NTScalarArray;
import org.epics.nt.NTScalarArrayBuilder;
import org.epics.nt.NTScalarBuilder;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldBuilder;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVFloat;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVShort;
import org.epics.pvdata.pv.PVShortArray;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdatabase.PVRecord;


public class S7DBAiFactory extends DBBaseFactory {
    
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
       
    @Override
    public DBRecord create(String recordName) {
        NTScalarBuilder ntScalarBuilder = NTScalar.createBuilder();
        FieldBuilder fb = fieldCreate.createFieldBuilder();
        
        Field cmd = fb.addNestedStructure("cmd").
                add("iMode", fieldCreate.createScalar(ScalarType.pvShort)).
                add("iErrorCode", fieldCreate.createScalar(ScalarType.pvShort)).                
                add("iStatus", fieldCreate.createScalar(ScalarType.pvShort)). 
                add("rActiveValue", fieldCreate.createScalar(ScalarType.pvFloat)). 
                add("rInputValue", fieldCreate.createScalar(ScalarType.pvFloat)).                 
                add("bPB_ResetError", fieldCreate.createScalar(ScalarType.pvBoolean)).                                 
                add("bPBEN_ResetError", fieldCreate.createScalar(ScalarType.pvBoolean)).    
                add("bError", fieldCreate.createScalar(ScalarType.pvBoolean)).                 
                createStructure();
        
        Field sts = fb.addNestedStructure("sts").                
                add("bLowLowAlarm", fieldCreate.createScalar(ScalarType.pvBoolean)).                                 
                add("bHighHighAlarm", fieldCreate.createScalar(ScalarType.pvBoolean)).    
                add("bInvalid", fieldCreate.createScalar(ScalarType.pvBoolean)).                 
                createStructure();        
        
        PVStructure pvStructure = ntScalarBuilder.
            value(ScalarType.pvShort).
            addDescriptor().
            add("cmd", cmd).
            add("sts", sts).                  
            add("id", fieldCreate.createScalar(ScalarType.pvString)).  
            add("offset", fieldCreate.createScalar(ScalarType.pvString)).                 
            add("scan_time", fieldCreate.createScalar(ScalarType.pvString)).
            add("scan_enable", fieldCreate.createScalar(ScalarType.pvBoolean)).
            add("write_enable", fieldCreate.createScalar(ScalarType.pvBoolean)).  
            add("write_value", fieldCreate.createScalar(ScalarType.pvShort)).                 
            addAlarm().
            addTimeStamp().
            addDisplay().
            addControl(). 
            createPVStructure();          
        DBRecord dbRecord = new DBS7DiRecord(recordName,pvStructure);      
        return dbRecord;
    }

           
    class DBS7DiRecord extends DBRecord implements PlcItemListener {   
    
        private int BUFFER_SIZE = 20;
        private static final String MONITOR_TF_FIELDS = "field(bPBEN_ResetError)";         
   
    
        private PVShort value; 
        private PVShort write_value;
        private PVBoolean write_enable; 
        
        private PVShort iMode; 
        private PVShort iErrorCode;
        private PVShort iStatus; 
        
        private PVFloat rActiveValue;
        private PVFloat rInputValue; 
        
        private PVBoolean bPB_ResetError;
        private PVBoolean bPBEN_ResetError;                
        private PVBoolean bError; 
        
        private PVBoolean bLowLowAlarm;   
        private PVBoolean bHighHighAlarm;
        private PVBoolean bInvalid; 
        
        
        byte byTemp;
    
        public DBS7DiRecord(String recordName,PVStructure pvStructure) {
            super(recordName, pvStructure);
            value = pvStructure.getShortField("value");
            write_value = pvStructure.getShortField("write_value");
            write_enable = pvStructure.getBooleanField("write_enable");
            
            iMode = pvStructure.getShortField("cmd/iMode");
            iErrorCode = pvStructure.getShortField("cmd/iErrorCode");
            iStatus = pvStructure.getShortField("cmd/iStatus");  
            
            rActiveValue = pvStructure.getFloatField("cmd/rActiveValue"); 
            rInputValue = pvStructure.getFloatField("cmd/rInputValue");             
            
            bPB_ResetError = pvStructure.getBooleanField("cmd/bPB_ResetError");
            bPBEN_ResetError = pvStructure.getBooleanField("cmd/bPBEN_ResetError");
            bError = pvStructure.getBooleanField("cmd/bError");   
            
            bLowLowAlarm = pvStructure.getBooleanField("cmd/bLowLowAlarm"); 
            bHighHighAlarm = pvStructure.getBooleanField("cmd/bHighHighAlarm"); 
            bInvalid = pvStructure.getBooleanField("cmd/bInvalid");             

        }    

        /**
         * Implement real time data to the record.
         * The main code is here.
         */
        public void process()
        {
            if (null != plcItem) {               
                if (write_enable.get()) {                          
                    write_value.put(value.get());                           
                    innerWriteBuffer.clear();                     
//                    innerWriteBuffer.writeShort(intToBcd(write_value.get()));
                    super.process();                      
                }
            }               
        }

        //udtHMI_DigitalInput
        @Override
        public void atach(final PlcItem plcItem) {
            this.plcItem = plcItem;
            //offset = this.getPVStructure().getIntField("offset").get() * Short.BYTES;  
            getOffset( this.getPVStructure().getStringField("offset").get());            
            innerBuffer = plcItem.getItemByteBuf().slice(byteOffset, BUFFER_SIZE);
            innerWriteBuffer = Unpooled.copiedBuffer(innerBuffer);
        }

        @Override
        public void detach() {
            this.plcItem  = null;
        }

        @Override
        public void update() {    
            if (null != plcItem) {
                innerBuffer.resetReaderIndex();
                iMode.put(innerBuffer.readShort());
                iErrorCode.put(innerBuffer.readShort());  
                iStatus.put(innerBuffer.readShort()); 
                
                rActiveValue.put(innerBuffer.readFloat()); 
                rInputValue.put(innerBuffer.readFloat());                 
                
                byTemp = innerBuffer.readByte();                
                bPB_ResetError.put(isBitSet(byTemp, 0));
                bPBEN_ResetError.put(isBitSet(byTemp, 1));                
                bError.put(isBitSet(byTemp, 2));  
                
                byTemp = innerBuffer.readByte();                 
                bLowLowAlarm.put(isBitSet(byTemp, 0));                  
                bHighHighAlarm.put(isBitSet(byTemp, 1));
                bInvalid.put(isBitSet(byTemp, 2)); 
                
            }
        }
        
        @Override
        public String getFieldsToMonitor() {
            return MONITOR_TF_FIELDS;
        }
        
        
    }
           
}
