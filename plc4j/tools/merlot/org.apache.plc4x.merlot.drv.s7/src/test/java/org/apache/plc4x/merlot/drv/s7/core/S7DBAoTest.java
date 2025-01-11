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
import static io.netty.buffer.Unpooled.buffer;
import java.util.UUID;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.spi.values.PlcRawByteArray;
import org.apache.plc4x.merlot.api.PlcItem;
import org.apache.plc4x.merlot.api.impl.PlcItemImpl;
import org.apache.plc4x.merlot.db.api.DBRecord;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVByte;
import org.epics.pvdata.pv.PVFloat;
import org.epics.pvdata.pv.PVShort;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdata.pv.PVStructure;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lerb
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S7DBAoTest {

    private static final Logger logger = LoggerFactory.getLogger(S7DBAoTest.class);
    private PlcValue plcValue;
    private static ByteBuf byteBuf;
    private PlcItem plcItem;
    private PVShort value;
    private PVShort write_value;
    private PVBoolean write_enable;

    private PVShort iMode;
    private PVShort iErrorCode;

    private PVFloat rValue;
    private PVFloat rAutoValue;
    private PVFloat rManualValue;
    private PVFloat rEstopValue;

    private PVBoolean bPB_ResetError;
    private PVBoolean bPBEN_ResetError;
    private PVBoolean bError;
    private PVBoolean bInterlock;

    private PVShort iEstopFunction;

    private PVBoolean bOutOfRange;
    private PVBoolean bConfigurationError;

    private PVByte bySpare;

    @BeforeAll
    public static void setUpClass() {
        /*
        Create an object Bytebuf
         */
        logger.info("Starting the testing of the analog output class");
        logger.info("Test Analog output for S7 plc");
        logger.info("Creating buffer to plcValue to Ao");
        byteBuf = buffer(100);

        byteBuf.setShort(0, 1234);   //imode
        byteBuf.setShort(2, 4321);   //iErrorCode
        byteBuf.setFloat(4, 3.1416F); //rValue
        byteBuf.setFloat(8, 3.1416F * 2); //rAutoValue
        byteBuf.setFloat(12, 3.1416F * 3); //rManualValue
        byteBuf.setFloat(16, 3.1416F * 4); //rEstopValue
        byteBuf.setByte(20, 15);
        byteBuf.setShort(22, 1234);//iEstopFunction

        byteBuf.setByte(24, 3);

        byteBuf.setByte(26, 111);
    }

    @AfterAll
    public static void tearDownClass() {
        logger.info("Ending the analog outputs class test");
    }

    @BeforeEach
    public void setUp() {
        /*
        defining an id and PlcItem
         */
        logger.info("Creating  plcValue and plcItem to Ao");
        String uuid = UUID.randomUUID().toString();
        plcValue = new PlcRawByteArray(byteBuf.array());
        plcItem = new PlcItemImpl.PlcItemBuilder("ITEM_DB42").
                setItemDescription("SIM DB42 S7").
                setItemId(uuid).
                setItemUid(UUID.fromString(uuid)).
                build();
        assertNotNull(plcItem);
        assertNotNull(plcValue);
    }

    @AfterEach
    public void tearDown() {
        plcItem = null;
        plcValue = null;
    }

    @Test
    @Order(1)
    public void dbAoRecord() {

        S7DBAoFactory AIFactory = new S7DBAoFactory();
        DBRecord AO_00 = AIFactory.create("AO_00");
        PVString pvStrOffset = AO_00.getPVRecordStructure().getPVStructure().getStringField("offset");
        pvStrOffset.put("0");

        plcItem.addItemListener(AO_00);
        plcItem.setPlcValue(plcValue);

        PVStructure pvStructureCmd = AO_00.getPVRecordStructure().getPVStructure().getStructureField("cmd");
        PVStructure pvStructureSts = AO_00.getPVRecordStructure().getPVStructure().getStructureField("sts");
        PVStructure pvStructurePar = AO_00.getPVRecordStructure().getPVStructure().getStructureField("par");

        value = AO_00.getPVRecordStructure().getPVStructure().getShortField("value");
        write_value = AO_00.getPVRecordStructure().getPVStructure().getShortField("write_value");
        write_enable = AO_00.getPVRecordStructure().getPVStructure().getBooleanField("write_enable");

        /*
        cmd
         */
        iMode = pvStructureCmd.getShortField("iMode");
        iErrorCode = pvStructureCmd.getShortField("iErrorCode");
        rValue = pvStructureCmd.getFloatField("rValue");
        rAutoValue = pvStructureCmd.getFloatField("rAutoValue");
        rManualValue = pvStructureCmd.getFloatField("rManualValue");
        rEstopValue = pvStructureCmd.getFloatField("rEstopValue");
        bPB_ResetError = pvStructureCmd.getBooleanField("bPB_ResetError");
        bPBEN_ResetError = pvStructureCmd.getBooleanField("bPBEN_ResetError");
        bError = pvStructureCmd.getBooleanField("bError");
        bInterlock = pvStructureCmd.getBooleanField("bInterlock");
        iEstopFunction = pvStructureCmd.getShortField("iEstopFunction");

        /*
        sts
         */
        bOutOfRange = pvStructureSts.getBooleanField("bOutOfRange");
        bConfigurationError = pvStructureSts.getBooleanField("bConfigurationError");

        /*
        par
         */
        bySpare = pvStructurePar.getByteField("bySpare");

        //Assertions
        logger.info("\n--------------STARTING TEST DBRECORD   Analog Outputs----------");
        logger.info(String.format("Prueba en  Analog outputs:\n (iMode esperado: 1234 == (iMode actual): %d)", iMode.get()));
        assertEquals(1234, iMode.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (iErrorCode esperado: 4321 == (iErrorCode actual): %d)", iErrorCode.get()));
        assertEquals(4321, iErrorCode.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (rValue esperado: 3.1416F == (rValue actual): %f)", rValue.get()));
        assertEquals(3.1416F, rValue.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (rAutoValue esperado: 6.2832F == (rAutoValue actual): %f)", rAutoValue.get()));
        assertEquals(3.1416F * 2, rAutoValue.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (rManualValue esperado: 9.4248F == (rManualValue actual): %f)", rManualValue.get()));
        assertEquals(3.1416F * 3, rManualValue.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (rEstopValue esperado: 12.5664F == (rEstopValue actual): %f)", rEstopValue.get()));
        assertEquals(3.1416F * 4, rEstopValue.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (bPB_ResetError esperado: true == (bPB_ResetError actual): %b)", bPB_ResetError.get()));
        assertEquals(true, bPB_ResetError.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (bPBEN_ResetError esperado: true == (bPBEN_ResetError actual): %b)", bPBEN_ResetError.get()));
        assertEquals(true, bPBEN_ResetError.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (bError esperado: true == (bError actual): %b)", bError.get()));
        assertEquals(true, bError.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (bInterlock esperado: true == (bInterlock actual): %b)", bInterlock.get()));
        assertEquals(true, bInterlock.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (iEstopFunction esperado: 1234 == (iEstopFunction actual): %d)", iEstopFunction.get()));
        assertEquals(1234, iEstopFunction.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (bOutOfRange esperado: true == (bOutOfRange actual): %b)", bOutOfRange.get()));
        assertEquals(true, bOutOfRange.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (bConfigurationError esperado: true == (bConfigurationError actual): %b)", bConfigurationError.get()));
        assertEquals(true, bConfigurationError.get());
        logger.info(String.format("Prueba en  Analog outputs:\n (bySpare esperado: 111 == (bySpare actual): %d)", bySpare.get()));
        assertEquals(111, bySpare.get());

        plcItem.setPlcValue(plcValue);

        logger.info("\nTEST Ao analog outputs SUCCESSFULLY COMPLETED ");
    }
}
