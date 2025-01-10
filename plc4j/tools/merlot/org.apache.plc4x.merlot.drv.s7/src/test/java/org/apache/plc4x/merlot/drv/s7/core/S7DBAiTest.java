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
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S7DBAiTest {

    private static final Logger logger = LoggerFactory.getLogger(S7DBAiTest.class);
    private static ByteBuf byteBuf;
    private PlcValue plcValue;
    private PlcItem plcItem;

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
        logger.info("Starting the testing of the analog input class");
        logger.info("Test Analog inputs for S7 plc");
        logger.info("Creating buffer to plcValue");
        byteBuf = buffer(512);
        byteBuf.setShort(0, 1234);               //iMOde
        byteBuf.setShort(2, 4321);               //iErrorCode
        byteBuf.setShort(4, 1010);               //iStatus
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
    }

    @AfterAll
    public static void tearDownClass() {
        logger.info("Ending the analog input class test");
    }

    @BeforeEach
    public void setUp() {
        logger.info("Creating  plcValue and plcItem to Ai");
        //Create PLCList for the items
        plcValue = new PlcRawByteArray(byteBuf.array());
        //Create the Item 
        String uuid = UUID.randomUUID().toString();
        plcItem = new PlcItemImpl.PlcItemBuilder("ITEM_DB42").
                setItemDescription("SIM DB42 S7").
                setItemId(uuid).
                setItemUid(UUID.fromString(uuid)).
                build();
    }

    @AfterEach
    public void tearDown() {
        plcItem = null;
        plcValue = null;
    }

    @Test
    @Order(2)
    public void FieldOffsetTest() {
        DBBaseFactory AIFactory = new S7DBAiFactory();
        DBRecord AI = AIFactory.create("AI_00");
        ArrayList<ImmutablePair<Integer, Byte>> fieldOffsets = AI.getFieldOffsets();

        assertNull(fieldOffsets.get(0));
        assertNull(fieldOffsets.get(1));
        assertNull(fieldOffsets.get(2));

        assertEquals(0, fieldOffsets.get(3).left);
        assertEquals((byte) -1, fieldOffsets.get(3).right);
        assertEquals(14, fieldOffsets.get(4).left);
        assertEquals((byte) -1, fieldOffsets.get(4).right);
        assertEquals(18, fieldOffsets.get(5).left);
        assertEquals((byte) 0, fieldOffsets.get(5).right);
        assertEquals(22, fieldOffsets.get(6).left);
        assertEquals((byte) -1, fieldOffsets.get(6).right);
        assertEquals(24, fieldOffsets.get(7).left);
        assertEquals((byte) -1, fieldOffsets.get(7).right);
        assertEquals(28, fieldOffsets.get(8).left);
        assertEquals((byte) -1, fieldOffsets.get(8).right);
        assertEquals(32, fieldOffsets.get(9).left);
        assertEquals((byte) -1, fieldOffsets.get(9).right);
        assertEquals(36, fieldOffsets.get(10).left);
        assertEquals((byte) -1, fieldOffsets.get(10).right);
        assertEquals(40, fieldOffsets.get(11).left);
        assertEquals((byte) -1, fieldOffsets.get(11).right);
        assertEquals(44, fieldOffsets.get(12).left);
        assertEquals((byte) -1, fieldOffsets.get(12).right);
        assertEquals(48, fieldOffsets.get(13).left);
        assertEquals((byte) -1, fieldOffsets.get(13).right);
        assertEquals(52, fieldOffsets.get(14).left);
        assertEquals((byte) -1, fieldOffsets.get(14).right);
        assertEquals(56, fieldOffsets.get(15).left);
        assertEquals((byte) -1, fieldOffsets.get(15).right);
        assertEquals(60, fieldOffsets.get(16).left);
        assertEquals((byte) -1, fieldOffsets.get(16).right);

        PVString pvStrOffset = AI.getPVRecordStructure().getPVStructure().getStringField("offset");
        pvStrOffset.put("1255");

        AI.ParseOffset("1255");

        assertEquals(AI.getBiteOffset(), -1);
        assertEquals(AI.getByteOffset(), 1255);

        AI.ParseOffset("255.7");
        assertEquals(AI.getBiteOffset(), 7);
        assertEquals(AI.getByteOffset(), 255);
        logger.info("TEST SUCCESSFULLY COMPLETED ");
    }

    // TODO add test methods here.
    // The methods must be annotated with annotation @Test. For example:
    //
    @Test
    @Order(1)
    public void DBRecordTest() {


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
        bLowLowAlarm = pvStructureSts.getBooleanField("bLowLowAlarm");
        bHighHighAlarm = pvStructureSts.getBooleanField("bHighHighAlarm");
        bInvalid = pvStructureSts.getBooleanField("bInvalid");

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

         logger.info("STARTING TEST DBRECORD AI");
        logger.info(String.format("Prueba en Ai:\n (iMode esperado: 1234 == (iMode actual): %d)", iMode.get()));
        assertEquals(1234, iMode.get());
        logger.info(String.format("Prueba en Ai:\n (iErrorCode esperado: 4321 == (iErrorCode actual): %d)", iErrorCode.get()));
        assertEquals(4321, iErrorCode.get());
        logger.info(String.format("Prueba en Ai:\n (iStatus esperado: 1010 == (iStatus actual): %d)", iStatus.get()));
        assertEquals(1010, iStatus.get());
        logger.info(String.format("Prueba en Ai:\n (rActiveValue esperado: 3.1416F == (rActiveValue actual): %f)", rActiveValue.get()));
        assertEquals(3.1416F, rActiveValue.get());
        logger.info(String.format("Prueba en Ai:\n (rInputValue esperado: 6.2832F == (rInputValue actual): %f)", rInputValue.get()));
        assertEquals(3.1416F * 2, rInputValue.get());
        logger.info(String.format("Prueba en Ai:\n (rManualValue esperado: 12.5654F == (rManualValue actual): %f)", rManualValue.get()));
        assertEquals(3.1416F * 4, rManualValue.get());
        logger.info(String.format("Prueba en Ai:\n (iSensorType esperado: 123 == (iSensorType actual): %d)", iSensorType.get()));
        assertEquals(123, iSensorType.get());
        logger.info(String.format("Prueba en Ai:\n (rInEngUnitsMin esperado: 25.1328F == (rInEngUnitsMin actual): %f)", rInEngUnitsMin.get()));
        assertEquals(3.1416F * 8, rInEngUnitsMin.get());
        logger.info(String.format("Prueba en Ai:\n (rInEngUnitsMax esperado: 31.416F == (rInEngUnitsMax actual): %f)", rInEngUnitsMax.get()));
        assertEquals(3.1416F * 10, rInEngUnitsMax.get());
        logger.info(String.format("Prueba en Ai:\n (rInLowLow esperado: 37.6992F == (rInLowLow actual): %f)", rInLowLow.get()));
        assertEquals(3.1416F * 12, rInLowLow.get());
        logger.info(String.format("Prueba en Ai:\n (rInLow esperado: 43.9824F == (rInLow actual): %f)", rInLow.get()));
        assertEquals(3.1416F * 14, rInLow.get());
        logger.info(String.format("Prueba en Ai:\n (rInHigh esperado: 50.2656F == (rInHigh actual): %f)", rInHigh.get()));
        assertEquals(3.1416F * 16, rInHigh.get());
       logger.info(String.format("Prueba en Ai:\n (rInHighHigh esperado: 56.5488F == (rInHighHigh actual): %f)", rInHighHigh.get()));
        assertEquals(3.1416F * 18, rInHighHigh.get());
        logger.info(String.format("Prueba en Ai:\n (rInLowLowDeadband esperado: 62.832F == (rInLowLowDeadband actual): %f)", rInLowLowDeadband.get()));
        assertEquals(3.1416F * 20, rInLowLowDeadband.get());
        logger.info(String.format("Prueba en Ai:\n (rInLowDeadband esperado: 69.1152F == (rInLowDeadband actual): %f)", rInLowDeadband.get()));
        assertEquals(3.1416F * 22, rInLowDeadband.get());
        logger.info(String.format("Prueba en Ai:\n (rInHighDeadband esperado: 75.3984F == (rInHighDeadband actual): %f)", rInHighDeadband.get()));
        assertEquals(3.1416F * 24, rInHighDeadband.get());
        logger.info(String.format("Prueba en Ai:\n (rInHighHighDeadband esperado: 87.9648F == (rInHighHighDeadband actual): %f)", rInHighHighDeadband.get()));
        assertEquals(3.1416F * 28, rInHighHighDeadband.get());

        //Test bits        
        byteBuf.setByte(18, 0x07);               //bPB_resetError = bPBEN_ResetError = bError = true;
        byteBuf.setByte(20, 0x07);               //LowLowAlarm = HighHighAlarm=Invalid
        plcItem.setPlcValue(plcValue);

        assertEquals(true, bPB_ResetError.get());
        assertEquals(true, bPBEN_ResetError.get());
        assertEquals(true, bError.get());
        assertEquals(true, bLowLowAlarm.get());
        assertEquals(true, bHighHighAlarm.get());
        assertEquals(true, bInvalid.get());

        byteBuf.setByte(18, 0x05);               //bPB_resetError =  bError = true; bPBEN_ResetError = false;
        byteBuf.setByte(20, 0x05);               //LowLowAlarm = Invalid; HighHighAlarm= = false;
        plcItem.setPlcValue(plcValue);

        assertEquals(true, bPB_ResetError.get());
        assertEquals(false, bPBEN_ResetError.get());
        assertEquals(true, bError.get());
        assertEquals(true, bLowLowAlarm.get());
        assertEquals(false, bHighHighAlarm.get());
        assertEquals(true, bInvalid.get());

        logger.info("TEST SUCCESSFULLY COMPLETED ");

    }
}
