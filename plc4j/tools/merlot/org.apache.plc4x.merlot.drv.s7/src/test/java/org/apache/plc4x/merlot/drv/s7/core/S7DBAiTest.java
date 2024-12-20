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
package org.apache.plc4x.merlot.drv.s7.core;


import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import static io.netty.buffer.Unpooled.buffer;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.spi.values.PlcBYTE;
import org.apache.plc4x.java.spi.values.PlcList;
import org.apache.plc4x.java.spi.values.PlcRawByteArray;
import org.apache.plc4x.merlot.api.PlcItem;
import org.apache.plc4x.merlot.api.impl.PlcItemImpl;
import org.apache.plc4x.merlot.api.impl.PlcItemImpl.PlcItemBuilder;
import org.apache.plc4x.merlot.db.api.DBRecord;
import org.apache.plc4x.merlot.db.core.DBBaseFactory;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVFloat;
import org.epics.pvdata.pv.PVShort;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;


public class S7DBAiTest {
    
    
    
        private PVShort value; 
        private PVShort write_value;
        private PVBoolean write_enable; 
        
        private PVShort iMode; 
        private PVShort iErrorCode;
        private PVShort iStatus; 
        
        private PVFloat rActiveValue;
        private PVFloat rInputValue; 
        private PVFloat rManualValue;         
        
        private PVBoolean bPB_ResetError;
        private PVBoolean bPBEN_ResetError;                
        private PVBoolean bError; 
        
        private PVBoolean bLowLowAlarm;   
        private PVBoolean bHighHighAlarm;
        private PVBoolean bInvalid; 
        
        private PVShort out_iMode;         
        private PVFloat out_rManualValue;         
        private PVBoolean out_bPB_ResetError; 
        
        private PVShort iSensorType;
        private PVFloat rInEngUnitsMin;
        private PVFloat rInEngUnitsMax;  
        private PVFloat rInLowLow; 
        private PVFloat rInLow;
        private PVFloat rInHigh;        
        private PVFloat rInHighHigh; 
        private PVFloat rInLowLowDeadband;         
        private PVFloat rInLowDeadband; 
        private PVFloat rInHighDeadband;
        private PVFloat rInHighHighDeadband;      
    
    
    
    public S7DBAiTest() {
    }
    
    @BeforeAll
    public static void setUpClass() {
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
    }
    
    @AfterEach
    public void tearDown() {
    }
    
     @Test
     public void FieldOffsetTest() { 
        DBBaseFactory  AIFactory = new S7DBAiFactory();
        DBRecord AI = AIFactory.create("AI_00");         
        ArrayList<ImmutablePair<Integer, Byte>> fieldOffsets = AI.getFieldOffsets();
        
        assertNull(fieldOffsets.get(0));
        assertNull(fieldOffsets.get(1));
        assertNull(fieldOffsets.get(2));

        assertEquals(fieldOffsets.get(3).left, 0); assertEquals(fieldOffsets.get(3).right, (byte) -1);
        assertEquals(fieldOffsets.get(4).left, 14); assertEquals(fieldOffsets.get(4).right, (byte) -1);        
        assertEquals(fieldOffsets.get(5).left, 18); assertEquals(fieldOffsets.get(5).right, (byte) 0); 
        assertEquals(fieldOffsets.get(6).left, 22); assertEquals(fieldOffsets.get(6).right, (byte) -1); 
        assertEquals(fieldOffsets.get(7).left, 24); assertEquals(fieldOffsets.get(7).right, (byte) -1); 
        assertEquals(fieldOffsets.get(8).left, 28); assertEquals(fieldOffsets.get(8).right, (byte) -1); 
        assertEquals(fieldOffsets.get(9).left, 32); assertEquals(fieldOffsets.get(9).right, (byte) -1); 
        assertEquals(fieldOffsets.get(10).left, 36); assertEquals(fieldOffsets.get(10).right, (byte) -1); 
        assertEquals(fieldOffsets.get(11).left, 40); assertEquals(fieldOffsets.get(11).right, (byte) -1); 
        assertEquals(fieldOffsets.get(12).left, 44); assertEquals(fieldOffsets.get(12).right, (byte) -1); 
        assertEquals(fieldOffsets.get(13).left, 48); assertEquals(fieldOffsets.get(13).right, (byte) -1); 
        assertEquals(fieldOffsets.get(14).left, 52); assertEquals(fieldOffsets.get(14).right, (byte) -1); 
        assertEquals(fieldOffsets.get(15).left, 56); assertEquals(fieldOffsets.get(15).right, (byte) -1); 
        assertEquals(fieldOffsets.get(16).left, 60); assertEquals(fieldOffsets.get(16).right, (byte) -1); 
        
        PVString pvStrOffset = AI.getPVRecordStructure().getPVStructure().getStringField("offset");
        pvStrOffset.put("1255");

        AI.ParseOffset("1255");
        
        assertEquals(AI.getBiteOffset(), -1);
        assertEquals(AI.getByteOffset(), 1255); 
        
        AI.ParseOffset("255.7");        
        assertEquals(AI.getBiteOffset(), 7);
        assertEquals(AI.getByteOffset(), 255);         
        
     }    

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
     @Test
     public void DBRecordTest() {    
         ByteBuf byteBuf = buffer(512);
         //Create PLCList for the items
         PlcValue plcValue = new PlcRawByteArray(byteBuf.array());
         
        
         //Create the Item 
        String uuid = UUID.randomUUID().toString();
        PlcItem plcItem =new PlcItemImpl.PlcItemBuilder("ITEM_DB42").
                                    setItemDescription("SIM DB42 S7").
                                    setItemId(uuid).
                                    setItemUid(UUID.fromString(uuid)).                                   
                                    build();  
        
        
        ByteBuf bufItem = plcItem.getItemByteBuf();
        
        byteBuf.setShort(0,1234);               //iMOde
        byteBuf.setShort(2,4321);               //iErrorCode
        byteBuf.setShort(4,1010);               //iStatus
        byteBuf.setFloat(6, 3.1416F);           //iActiveValue
        byteBuf.setFloat(10, 3.1416F * 2);      //rInputValue
        byteBuf.setFloat(14, 3.1416F * 4);      //rManualValue
        byteBuf.setShort(22, 123);              //iSensorType
        byteBuf.setFloat(24, 3.1416F * 8);      //rInEngUnitsMin
        byteBuf.setFloat(28, 3.1416F * 10);     //rInEngUnitsMax         
        byteBuf.setFloat(32, 3.1416F * 12);     //rInLowLow         
        byteBuf.setFloat(36, 3.1416F * 14);     //rInLow
        byteBuf.setFloat(40, 3.1416F * 16);     //rInHigh
        byteBuf.setFloat(44, 3.1416F * 18);     //rInHighHigh
        byteBuf.setFloat(48, 3.1416F * 20);     //rInLowLowDeadband
        byteBuf.setFloat(52, 3.1416F * 22);     //rInLowDeadband
        byteBuf.setFloat(56, 3.1416F * 24);     //rInHighDeadband
        byteBuf.setFloat(60, 3.1416F * 28);     //rInHighHighDeadband        
        
        byteBuf.setByte(511, 123);
        
        
        S7DBAiFactory AIFactory = new S7DBAiFactory();
        DBRecord AI_00 = AIFactory.create("AI_00"); 
        PVString pvStrOffset = AI_00.getPVRecordStructure().getPVStructure().getStringField("offset");
        pvStrOffset.put("0");

        AI_00.atach(plcItem);
        
        plcItem.addItemListener(AI_00); 
        plcItem.setPlcValue(plcValue);        
        
        PVStructure pvStructureCmd = AI_00.getPVRecordStructure().getPVStructure().getStructureField("cmd"); 
        PVStructure pvStructureSts = AI_00.getPVRecordStructure().getPVStructure().getStructureField("sts");         
        PVStructure pvStructurePar = AI_00.getPVRecordStructure().getPVStructure().getStructureField("par");
        
        value = AI_00.getPVRecordStructure().getPVStructure().getShortField("value"); 
        write_value = AI_00.getPVRecordStructure().getPVStructure().getShortField("write_value");
        write_enable = AI_00.getPVRecordStructure().getPVStructure().getBooleanField("write_enable");

        //Read command values            
        iMode = pvStructureCmd.getShortField("iMode");
        iErrorCode = pvStructureCmd.getShortField("iErrorCode");
        iStatus = pvStructureCmd.getShortField("iStatus");              
        rActiveValue = pvStructureCmd.getFloatField("rActiveValue"); 
        rInputValue = pvStructureCmd.getFloatField("rInputValue");                         
        rManualValue = pvStructureCmd.getFloatField("rManualValue");
        bPB_ResetError = pvStructureCmd.getBooleanField("bPB_ResetError");
        bPBEN_ResetError = pvStructureCmd.getBooleanField("bPBEN_ResetError");
        bError = pvStructureCmd.getBooleanField("bError");               

        //Read status values
        bLowLowAlarm =  pvStructureSts.getBooleanField("bLowLowAlarm");
        bHighHighAlarm =  pvStructureSts.getBooleanField("bHighHighAlarm");
        bInvalid =  pvStructureSts.getBooleanField("bInvalid");

        //Write command and parameters values
        out_iMode = pvStructurePar.getShortField("iMode");     
        out_rManualValue = pvStructurePar.getFloatField("rManualValue");  
        out_bPB_ResetError = pvStructurePar.getBooleanField("bPB_ResetError");

        iSensorType = pvStructurePar.getShortField("iSensorType");
        rInEngUnitsMin = pvStructurePar.getFloatField("rInEngUnitsMin");
        rInEngUnitsMax = pvStructurePar.getFloatField("rInEngUnitsMax");  
        rInLowLow = pvStructurePar.getFloatField("rInLowLow"); 
        rInLow = pvStructurePar.getFloatField("rInLow");
        rInHigh = pvStructurePar.getFloatField("rInHigh");       
        rInHighHigh = pvStructurePar.getFloatField("rInHighHigh");
        rInLowLowDeadband = pvStructurePar.getFloatField("rInLowLowDeadband");        
        rInLowDeadband = pvStructurePar.getFloatField("rInLowDeadband"); 
        rInHighDeadband = pvStructurePar.getFloatField("rInHighDeadband");
        rInHighHighDeadband = pvStructurePar.getFloatField("rInHighHighDeadband");          
        
                    
        assertEquals(iMode.get(), 1234);
        assertEquals(iErrorCode.get(), 4321);
        assertEquals(iStatus.get(), 1010);        
        assertEquals(rActiveValue.get(), 3.1416F);
        assertEquals(rInputValue.get(), 3.1416F * 2);        
        assertEquals(rManualValue.get(), 3.1416F * 4); 
        assertEquals(iSensorType.get(), 123);         
        assertEquals(rInEngUnitsMin.get(), 3.1416F * 8);         
        assertEquals(rInEngUnitsMax.get(), 3.1416F * 10);
        assertEquals(rInLowLow.get(), 3.1416F * 12);
        assertEquals(rInLow.get(), 3.1416F * 14);         
        assertEquals(rInHigh.get(), 3.1416F * 16);
        assertEquals(rInHighHigh.get(), 3.1416F * 18); 
        assertEquals(rInLowLowDeadband.get(), 3.1416F * 20); 
        assertEquals(rInLowDeadband.get(), 3.1416F * 22); 
        assertEquals(rInHighDeadband.get(), 3.1416F * 24); 
        assertEquals(rInHighHighDeadband.get(), 3.1416F * 28);  
        
        //Test bits        
        byteBuf.setByte(18,0x07);               //bPB_resetError = bPBEN_ResetError = bError = true;
        byteBuf.setByte(20,0x07);               //LowLowAlarm = HighHighAlarm=Invalid
        plcItem.setPlcValue(plcValue); 
        
        assertEquals(bPB_ResetError.get(), true);
        assertEquals(bPBEN_ResetError.get(), true); 
        assertEquals(bError.get(), true);  
        assertEquals(bLowLowAlarm.get(), true);
        assertEquals(bHighHighAlarm.get(), true); 
        assertEquals(bInvalid.get(), true); 
        
        byteBuf.setByte(18,0x05);               //bPB_resetError =  bError = true; bPBEN_ResetError = false;
        byteBuf.setByte(20,0x05);               //LowLowAlarm = Invalid; HighHighAlarm= = false;
        plcItem.setPlcValue(plcValue); 

        assertEquals(bPB_ResetError.get(), true);
        assertEquals(bPBEN_ResetError.get(), false); 
        assertEquals(bError.get(), true);  
        assertEquals(bLowLowAlarm.get(), true);
        assertEquals(bHighHighAlarm.get(), false); 
        assertEquals(bInvalid.get(), true);         
        
     }
}
