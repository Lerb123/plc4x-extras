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

import io.netty.buffer.ByteBuf;
import static io.netty.buffer.Unpooled.buffer;
import java.util.UUID;
import static javax.management.Query.value;
import org.apache.plc4x.java.api.value.PlcValue;
import org.apache.plc4x.java.spi.values.PlcRawByteArray;
import org.apache.plc4x.merlot.api.PlcItem;
import org.apache.plc4x.merlot.api.impl.PlcItemImpl;
import org.apache.plc4x.merlot.db.api.DBRecord;
import org.epics.pvdata.pv.PVString;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author lerb
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S7DBDiTest {

    private static final Logger logger = LoggerFactory.getLogger(S7DBDiTest.class);
    private static ByteBuf byteBuf;
    private PlcValue plcValue;
    private PlcItem plcItem;
    private DBRecord DI_00;

    @BeforeAll
    public static void setUpClass() {
        logger.info("Starting the testing of the digital input class");
        logger.info("Test Digital Input for S7 plc");
        logger.info("Creating buffer to plcValue");
        byteBuf = buffer(5);

    }

    @AfterAll
    public static void tearDownClass() {
        logger.info("Ending the Digital Input class test");
    }

    @BeforeEach
    public void setUp() {
        logger.info("Creating  plcValue and plcItem to Digital Input");
        //Create PLCList for the items
        plcValue = new PlcRawByteArray(byteBuf.array());
        //Create the Item 
        String uuid = UUID.randomUUID().toString();
        plcItem = new PlcItemImpl.PlcItemBuilder("ITEM_DB42").
                setItemDescription("SIM DB42 S7").
                setItemId(uuid).
                setItemUid(UUID.fromString(uuid)).
                build();
        assertNotNull(plcItem);
        assertNotNull(plcValue);

        //DBRecord associated with each particular test
        S7DBDiFactory DiFactory = new S7DBDiFactory();
        DI_00 = DiFactory.create("DI_00");

    }

    @AfterEach
    public void tearDown() {
        plcItem = null;
        plcValue = null;
    }

    @Test
    @Order(1)
    public void DBRecordTest() {
        PVString pvStrOffset = DI_00.getPVRecordStructure().getPVStructure().getStringField("offset");
        pvStrOffset.put("0");
        plcItem.addItemListener(DI_00);
        plcItem.setPlcValue(plcValue);

        logger.info("\n--------------STARTING TEST DBRECORD AI----------");
//        logger.info(String.format("", ));

    }

    @Test
    @Order(2)
    public void FieldOffsetTest() {

//        ArrayList<ImmutablePair<Integer, Byte>> fieldOffsets = DTime_00.getFieldOffsets();
//        logger.info(String.format("Number of items allowed to be monitored:(Value expected: 2) == (Value actual: %d)", fieldOffsets.size()));
    }
}
