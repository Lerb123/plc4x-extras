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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.plc4x.merlot.api.PlcItem;
import org.apache.plc4x.merlot.api.PlcItemListener;
import org.apache.plc4x.merlot.db.api.DBRecord;
import org.apache.plc4x.merlot.db.core.DBBaseFactory;
import org.epics.nt.NTScalar;
import org.epics.nt.NTScalarBuilder;
import org.epics.pvdata.factory.FieldFactory;
import org.epics.pvdata.pv.Field;
import org.epics.pvdata.pv.FieldBuilder;
import org.epics.pvdata.pv.FieldCreate;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVFloat;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVShort;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.ScalarType;

public class S7DBValveAnalogFactory extends DBBaseFactory {

    private static FieldCreate fieldCreate = FieldFactory.getFieldCreate();

    @Override
    public DBRecord create(String recordName) {
        final NTScalarBuilder ntScalarBuilder = NTScalar.createBuilder();
        final FieldBuilder fb = fieldCreate.createFieldBuilder();

        Field cmd = fb.setId("cmd").
                add("iMode", fieldCreate.createScalar(ScalarType.pvShort)).
                add("iErrorCode", fieldCreate.createScalar(ScalarType.pvShort)).
                add("iStatus", fieldCreate.createScalar(ScalarType.pvShort)).
                add("rManualSP", fieldCreate.createScalar(ScalarType.pvFloat)).
                add("rAutoSP", fieldCreate.createScalar(ScalarType.pvFloat)).
                add("rEstopSP", fieldCreate.createScalar(ScalarType.pvFloat)).
                add("rActual", fieldCreate.createScalar(ScalarType.pvFloat)).
                add("bPB_ResetError", fieldCreate.createScalar(ScalarType.pvBoolean)).
                add("bPBEN_ResetError", fieldCreate.createScalar(ScalarType.pvBoolean)).
                add("bError", fieldCreate.createScalar(ScalarType.pvBoolean)).
                add("bInterlock", fieldCreate.createScalar(ScalarType.pvBoolean)).
                add("iEstopFunction", fieldCreate.createScalar(ScalarType.pvShort)).
                createStructure();

        Field sts = fb.setId("sts").
                add("InvalidFeedback", fieldCreate.createScalar(ScalarType.pvBoolean)).
                createStructure();

        Field par = fb.setId("par").
                add("tTimeOut", fieldCreate.createScalar(ScalarType.pvInt)).
                createStructure();

        PVStructure pvStructure = ntScalarBuilder.
                value(ScalarType.pvShort).
                addDescriptor().
                add("cmd", cmd).
                add("sts", sts).
                add("par", par).
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

        DBRecord dbRecord = new DBS7ValveRecord(recordName, pvStructure);

        return dbRecord;
    }

    class DBS7ValveRecord extends DBRecord implements PlcItemListener {

        private int BUFFER_SIZE = 31;
        private static final String MONITOR_TF_FIELDS = "field(write_enable, par{tTimeOut})";

        private PVShort value;
        private PVShort write_value;
        private PVBoolean write_enable;

        private PVShort iMode;
        private PVShort iErrorCode;
        private PVShort iStatus;

        private PVFloat rManualSP;
        private PVFloat rAutoSP;
        private PVFloat rEstopSP;
        private PVFloat rActual;
        private PVBoolean bPB_ResetError;
        private PVBoolean bPBEN_ResetError;
        private PVBoolean bError;

        private PVBoolean bInterlock;
        private PVShort iEstopFunction;

        private PVBoolean InvalidFeedback;
        private PVInt tTimeOut;

        byte byTemp;

        public DBS7ValveRecord(String recordName, PVStructure pvStructure) {
            super(recordName, pvStructure);

            bFirtsRun = true;

            fieldOffsets = new ArrayList<>();
            fieldOffsets.add(0, null);
            fieldOffsets.add(1, null);
            fieldOffsets.add(2, null);
            fieldOffsets.add(3, new ImmutablePair(0, -1));
            fieldOffsets.add(4, new ImmutablePair(6, 0));
            fieldOffsets.add(5, new ImmutablePair(6, 1));
            fieldOffsets.add(6, new ImmutablePair(6, 2));

            value = pvStructure.getShortField("value");
            write_value = pvStructure.getShortField("write_value");
            write_enable = pvStructure.getBooleanField("write_enable");

            //Read command values
            PVStructure pvStructureCmd = pvStructure.getStructureField("cmd");
            iMode = pvStructureCmd.getShortField("iMode");
            iErrorCode = pvStructureCmd.getShortField("iErrorCode");
            iStatus = pvStructureCmd.getShortField("iStatus");
            rManualSP = pvStructureCmd.getFloatField("rManualSP");
            rAutoSP = pvStructureCmd.getFloatField("rAutoSP");
            rEstopSP = pvStructureCmd.getFloatField("rEstopSP");
            rActual = pvStructureCmd.getFloatField("rActual");
            bPB_ResetError = pvStructureCmd.getBooleanField("bPB_ResetError");
            bPBEN_ResetError = pvStructureCmd.getBooleanField("bPBEN_ResetError");
            bError = pvStructureCmd.getBooleanField("bError");
            bInterlock = pvStructureCmd.getBooleanField("bInterlock");
            iEstopFunction = pvStructureCmd.getShortField("iEstopFunction");

            //Read status values            
            PVStructure pvStructureSts = pvStructure.getStructureField("sts");
            InvalidFeedback = pvStructureSts.getBooleanField("InvalidFeedback");

            //Write command values            
            PVStructure pvStructureOut = pvStructure.getStructureField("par");
            tTimeOut = pvStructureOut.getIntField("tTimeOut");

        }

        /**
         * For other special types of data, adaptation must be made here to
         * write to the PLC.
         *
         * 1. In the first write all fields are written 2. In the second one
         * only the changes are written.
         *
         */
        public void process() {
        //put logic here
        }

        //udtHMI_DigitalInput
        @Override
        public void atach(final PlcItem plcItem) {
            this.plcItem = plcItem;
            ParseOffset(this.getPVStructure().getStringField("offset").get());
            innerBuffer = plcItem.getItemByteBuf().slice(byteOffset, BUFFER_SIZE);
            innerWriteBuffer = Unpooled.copiedBuffer(innerBuffer);
        }

        @Override
        public void detach() {
            this.plcItem = null;
        }

        @Override
        public void update() {
            if (null != plcItem) {
                innerBuffer.resetReaderIndex();

                /*
                cmd
                 */
                iMode.put(innerBuffer.getShort(0));
                iErrorCode.put(innerBuffer.getShort(2));
                iStatus.put(innerBuffer.getShort(4));

                rManualSP.put(innerBuffer.getFloat(6));
                rAutoSP.put(innerBuffer.getFloat(10));
                rEstopSP.put(innerBuffer.getFloat(14));
                rActual.put(innerBuffer.getFloat(18));

                byTemp = innerBuffer.getByte(22);
                bPB_ResetError.put(isBitSet(byTemp, 0));
                bPBEN_ResetError.put(isBitSet(byTemp, 1));
                bError.put(isBitSet(byTemp, 2));
                bInterlock.put(isBitSet(byTemp, 3));

                iEstopFunction.put(innerBuffer.getShort(24));

                /*
                sts
                 */
                byTemp = innerBuffer.getByte(26);
                InvalidFeedback.put(isBitSet(byTemp, 0));

                /*
                par
                 */
                tTimeOut.put(innerBuffer.getInt(28));

            }
        }

        @Override
        public String getFieldsToMonitor() {
            return MONITOR_TF_FIELDS;
        }

    }

}
