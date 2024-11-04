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
import java.util.ArrayList;
import org.apache.commons.lang3.tuple.MutablePair;
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
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVShort;
import org.epics.pvdata.pv.PVShortArray;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;
import org.epics.pvdatabase.PVRecord;


public class S7DBMotorFactory extends DBBaseFactory {
    
    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();
       
    @Override
    public DBRecord create(String recordName) {
        NTScalarBuilder ntScalarBuilder = NTScalar.createBuilder();
        FieldBuilder fb = fieldCreate.createFieldBuilder();
        
        Field cmd = fb.setId("cmd_t").
                add("iMode", fieldCreate.createScalar(ScalarType.pvShort)).
                add("iErrorCode", fieldCreate.createScalar(ScalarType.pvShort)).                
                add("iStatus", fieldCreate.createScalar(ScalarType.pvShort)).                  
                add("bPB_ResetError", fieldCreate.createScalar(ScalarType.pvBoolean)).  
                add("bPB_Forward", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bPB_Reverse", fieldCreate.createScalar(ScalarType.pvBoolean)).   
                add("bPB_Stop", fieldCreate.createScalar(ScalarType.pvBoolean)).                 
                add("bPBEN_ResetError", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bPBEN_Forward", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bPBEN_Reverse", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bPBEN_Stop", fieldCreate.createScalar(ScalarType.pvBoolean)).  
                add("bForwardOn", fieldCreate.createScalar(ScalarType.pvBoolean)).                
                add("bReverseOn", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bSignalForward", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bSignalReverse", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bError", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bInterlock", fieldCreate.createScalar(ScalarType.pvBoolean)).
                createStructure();
                
        
        Field sts = fb.setId("sts_t").               
                add("bMotorProtectorTripped", fieldCreate.createScalar(ScalarType.pvBoolean)).
                add("bLocalDisconnectOff", fieldCreate.createScalar(ScalarType.pvBoolean)).
                add("bClutchTripped", fieldCreate.createScalar(ScalarType.pvBoolean)).
                add("bNoSignalForward", fieldCreate.createScalar(ScalarType.pvBoolean)).
                add("bNoSignalReverse", fieldCreate.createScalar(ScalarType.pvBoolean)).
                add("bMotorNotStopped", fieldCreate.createScalar(ScalarType.pvBoolean)).                
                createStructure();   
        
        Field out =  fb.setId("output_t").   
                add("iMode", fieldCreate.createScalar(ScalarType.pvShort)).                
                add("bPBEN_ResetError", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bPBEN_Forward", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bPBEN_Reverse", fieldCreate.createScalar(ScalarType.pvBoolean)). 
                add("bPBEN_Stop", fieldCreate.createScalar(ScalarType.pvBoolean)).  
                createStructure();           
        
        PVStructure pvStructure = ntScalarBuilder.
            value(ScalarType.pvShort).
            addDescriptor().
            add("cmd", cmd).
            add("sts", sts).
            add("out", out).                
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
        
        DBRecord dbRecord = new DBS7MotorRecord(recordName,pvStructure);      
        
        return dbRecord;
    }

                                
    class DBS7MotorRecord extends DBRecord implements PlcItemListener {   
        
        private int BUFFER_SIZE = 8;
        private static final String MONITOR_TF_FIELDS = "field(write_enable, out{iMode, bPBEN_ResetError, bPBEN_Forward, bPBEN_Reverse, bPBEN_Stop})";
        
        
        private PVShort value; 
        private PVShort write_value;
        private PVBoolean write_enable; 
        
        private PVShort iMode; 
        private PVShort iErrorCode; 
        private PVShort iStatus;   

        private PVBoolean bPB_ResetError; 
        private PVBoolean bPB_Forward; 
        private PVBoolean bPB_Reverse; 
        private PVBoolean bPB_Stop;     
        private PVBoolean bPBEN_ResetError;         
        private PVBoolean bPBEN_Forward;
        private PVBoolean bPBEN_Reverse;
        private PVBoolean bPBEN_Stop;
        private PVBoolean bForwardOn;
        private PVBoolean bReverseOn;
        private PVBoolean bSignalForward;
        private PVBoolean bSignalReverse;
        private PVBoolean bError; 
        private PVBoolean bInterlock; 

        private PVBoolean bMotorProtectorTripped;         
        private PVBoolean bLocalDisconnectOff;  
        private PVBoolean bClutchTripped;  
        private PVBoolean bNoSignalForward;  
        private PVBoolean bNoSignalReverse;
        private PVBoolean bMotorNotStopped;  
        
        private PVShort out_iMode;         
        private PVBoolean out_bPBEN_ResetError;         
        private PVBoolean out_bPBEN_Forward;
        private PVBoolean out_bPBEN_Reverse;
        private PVBoolean out_bPBEN_Stop;        
        
        byte byTemp;
    
        public DBS7MotorRecord(String recordName,PVStructure pvStructure) {
            super(recordName, pvStructure);
            
            bFirtsRun = true;
            
            offsets = new ArrayList<>();
            offsets.add(0, null);
            offsets.add(1, new MutablePair(6,5));
            offsets.add(2, null); 
            offsets.add(3, new MutablePair(6,7));   
            offsets.add(4, new MutablePair(6,7)); 
            offsets.add(5, new MutablePair(6,7));             
            offsets.add(6, new MutablePair(6,7));                         
            
            value = pvStructure.getShortField("value");
            write_value = pvStructure.getShortField("write_value");
            write_enable = pvStructure.getBooleanField("write_enable");
            
            PVStructure pvStructureCmd = pvStructure.getStructureField("cmd");            
            iMode = pvStructureCmd.getShortField("iMode");
            iErrorCode = pvStructureCmd.getShortField("iErrorCode");            
            iStatus = pvStructureCmd.getShortField("iStatus");
          
            bPB_ResetError = pvStructureCmd.getBooleanField("bPB_ResetError");            
            bPB_Forward = pvStructureCmd.getBooleanField("bPB_Forward");
            bPB_Reverse = pvStructureCmd.getBooleanField("bPB_Reverse");            
            bPB_Stop = pvStructureCmd.getBooleanField("bPB_Stop");
            bPBEN_ResetError = pvStructureCmd.getBooleanField("bPBEN_ResetError"); 
            bPBEN_Forward = pvStructureCmd.getBooleanField("bPBEN_Forward"); 
            bPBEN_Reverse = pvStructureCmd.getBooleanField("bPBEN_Reverse"); 
            bPBEN_Stop = pvStructureCmd.getBooleanField("bPBEN_Stop");  
            bForwardOn = pvStructureCmd.getBooleanField("bForwardOn");
            bReverseOn = pvStructureCmd.getBooleanField("bReverseOn");  
            bSignalForward = pvStructureCmd.getBooleanField("bSignalForward");
            bSignalReverse = pvStructureCmd.getBooleanField("bSignalReverse"); 
            bError = pvStructureCmd.getBooleanField("bError");
            bInterlock = pvStructureCmd.getBooleanField("bInterlock"); 
            
            PVStructure pvStructureSts = pvStructure.getStructureField("sts");
            bMotorProtectorTripped = pvStructureSts.getBooleanField("bMotorProtectorTripped");
            bLocalDisconnectOff = pvStructureSts.getBooleanField("bLocalDisconnectOff"); 
            bClutchTripped = pvStructureSts.getBooleanField("bClutchTripped");
            bNoSignalForward = pvStructureSts.getBooleanField("bNoSignalForward");
            bNoSignalReverse = pvStructureSts.getBooleanField("bNoSignalReverse");
            bMotorNotStopped = pvStructureSts.getBooleanField("bMotorNotStopped"); 
            
            PVStructure pvStructureOut = pvStructure.getStructureField("out"); 
            out_iMode = pvStructureOut.getShortField("iMode");
            out_bPBEN_ResetError = pvStructureOut.getBooleanField("bPBEN_ResetError"); 
            out_bPBEN_Forward = pvStructureOut.getBooleanField("bPBEN_Forward"); 
            out_bPBEN_Reverse = pvStructureOut.getBooleanField("bPBEN_Reverse"); 
            out_bPBEN_Stop = pvStructureOut.getBooleanField("bPBEN_Stop");              

        }    

        /**
         * Implement real time data to the record.
         * The main code is here.
         */
        public void process() {
            if (bFirtsRun) {
                out_iMode.put(iMode.get());               
            } else{
             if (iMode.get() != out_iMode.get()) out_iMode.put(iMode.get());  
            }
        }

        //udtHMI_DigitalInput
        @Override
        public void atach(final PlcItem plcItem) {
            this.plcItem = plcItem;
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
                
                byTemp = innerBuffer.readByte();                
                bPB_ResetError.put(isBitSet(byTemp, 0));
                bPB_Forward.put(isBitSet(byTemp, 1));                
                bPB_Reverse.put(isBitSet(byTemp, 2));  
                bPB_Stop.put(isBitSet(byTemp, 3));                  
                bPBEN_ResetError.put(isBitSet(byTemp, 4));
                
                bPBEN_Forward.put(isBitSet(byTemp, 5));
                
                bPBEN_Reverse.put(isBitSet(byTemp, 6));                 
                bPBEN_Stop.put(isBitSet(byTemp, 7));  
                
                byTemp = innerBuffer.readByte(); 
                bForwardOn.put(isBitSet(byTemp, 0));                 
                bReverseOn.put(isBitSet(byTemp, 1));
                bSignalForward.put(isBitSet(byTemp, 2));
                bSignalReverse.put(isBitSet(byTemp, 3));
                bError.put(isBitSet(byTemp, 4));  
                bInterlock.put(isBitSet(byTemp, 5));

                byTemp = innerBuffer.readByte();
                bMotorProtectorTripped.put(isBitSet(byTemp, 0));
                bLocalDisconnectOff.put(isBitSet(byTemp, 1));
                bClutchTripped.put(isBitSet(byTemp, 2));
                bNoSignalForward.put(isBitSet(byTemp, 3));
                bNoSignalReverse.put(isBitSet(byTemp, 4));
                bMotorNotStopped .put(isBitSet(byTemp, 5));
                
            }
        }
        
        @Override
        public String getFieldsToMonitor() {
            return MONITOR_TF_FIELDS;
        }
    
        
    }
           
}
