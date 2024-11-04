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
package org.apache.plc4x.merlot.drv.s7.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.apache.plc4x.merlot.api.PlcItem;
import org.apache.plc4x.merlot.db.api.DBRecord;
import org.apache.plc4x.merlot.db.api.DBWriterHandler;
import org.epics.pvdata.copy.CreateRequest;
import org.epics.pvdata.misc.BitSet;
import org.epics.pvdata.monitor.Monitor;
import org.epics.pvdata.monitor.MonitorElement;
import org.epics.pvdata.pv.MessageType;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVByte;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVField;
import org.epics.pvdata.pv.PVFloat;
import org.epics.pvdata.pv.PVInt;
import org.epics.pvdata.pv.PVLong;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVShort;
import org.epics.pvdata.pv.PVStructure;
import org.epics.pvdata.pv.PVUByte;
import org.epics.pvdata.pv.PVUInt;
import org.epics.pvdata.pv.PVUShort;
import static org.epics.pvdata.pv.ScalarType.pvBoolean;
import static org.epics.pvdata.pv.ScalarType.pvByte;
import static org.epics.pvdata.pv.ScalarType.pvDouble;
import static org.epics.pvdata.pv.ScalarType.pvFloat;
import static org.epics.pvdata.pv.ScalarType.pvInt;
import static org.epics.pvdata.pv.ScalarType.pvLong;
import static org.epics.pvdata.pv.ScalarType.pvShort;
import static org.epics.pvdata.pv.ScalarType.pvUInt;
import org.epics.pvdata.pv.Status;
import org.epics.pvdata.pv.Structure;
import org.epics.pvdatabase.pva.MonitorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
* This service is responsible for calling the writing of a PvRecord through 
* the associated PlcItem.
*/
public class S7DBWriterHandlerImpl implements DBWriterHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(S7DBWriterHandlerImpl.class);
    private  CreateRequest createRequest = CreateRequest.create();
    private Map<Monitor, DBRecord> recordMonitors = new HashMap<>();
    
    private MonitorElement element  = null;
    private PVStructure structure   = null;
    private BitSet changedBitSet    = null;
    private BitSet overrunBitSet    = null;
    
    @Override
    public void monitorConnect(Status status, Monitor monitor, Structure structure) {

    }  

    @Override
    public void monitorEvent(Monitor monitor) {
        try 
        {
            element = monitor.poll();
            structure = element.getPVStructure();
            changedBitSet = element.getChangedBitSet();
            overrunBitSet = element.getOverrunBitSet();
                      
            if ((recordMonitors.containsKey(monitor)) && 
                 structure.getBooleanField("write_enable").get()) {
                
                System.out.println("La Estructura: " + structure);
                System.out.println("ChangeBitSet:  " + changedBitSet);
                System.out.println("OverrunBitSet: " + overrunBitSet);

                final DBRecord dbRecord = recordMonitors.get(monitor);
                final Optional<PlcItem> optPlcItem = dbRecord.getPlcItem();

                PVField[] fields = new PVField[structure.getNumberFields()];
                
                
                if (optPlcItem.isPresent()) {
                    System.out.println("Numero de bits cambiados: " + changedBitSet.cardinality());
                    System.out.println("Numero de campos: " + structure.getNumberFields());                                     
                    
                    //Tansform the tree to lineal array of fields
                    //I avoid recursion
                    int i = 1;
                    for (PVField pvField:structure.getPVFields()) {
                        System.out.println("I: " + i);
                        fields[i] = pvField;
                        if (pvField instanceof PVStructure) {
                            System.out.println(">Es una estructura");                        
                            final PVStructure pvStructure = (PVStructure) pvField;
                            for (PVField f:pvStructure.getPVFields()){
                                i++;
                                fields[i] = f;
                            }
                        }
                        i++;
                    }
                
                    System.out.println("Campos que cambiaron:");
                    int index = changedBitSet.nextSetBit(0);
                    for (i = 0; i < changedBitSet.cardinality(); i++) {
                        System.out.println("Indice encontrado: " + index);
                        System.out.println("PVField: " + fields[index]);
                        ByteBuf byteBuf = null;
                        if (fields[index] instanceof PVScalar){
                            //Capturo la informacion en un ByteBuf
                            final PVField f = fields[index]; 
                            final PVScalar pvScalar = (PVScalar) fields[index];
                            byteBuf = Unpooled.buffer(Double.BYTES);
                            switch(pvScalar.getScalar().getScalarType()) {
                                case pvBoolean:
                                    System.out.println("b");
                                    byteBuf.writeBoolean(((PVBoolean) f).get());
                                    break;
                                case pvByte:
                                    System.out.println("by"); 
                                    byteBuf.writeByte(((PVByte) f).get());                                   
                                    break;  
                                case pvDouble:
                                    System.out.println("dou");
                                    byteBuf.writeDouble(((PVDouble) f).get());                                     
                                    break; 
                                case pvFloat:
                                    System.out.println("f"); 
                                    byteBuf.writeFloat(((PVFloat) f).get());                                     
                                    break; 
                                case pvInt:
                                    System.out.println("int");
                                    byteBuf.writeInt(((PVInt) f).get());
                                    break; 
                                case pvLong:
                                    System.out.println("l");  
                                    byteBuf.writeLong(((PVLong) f).get());                                    
                                    break; 
                                case pvShort:
                                    System.out.println("sh"); 
                                    byteBuf.writeShort(((PVShort) f).get());                                     
                                    break; 
                                case pvString:
                                    System.out.println("str");  
                                    //byteBuf.write(((PVShort) f).get());                                     
                                    break;  
                                case pvUByte:
                                    System.out.println("ub");  
                                    byteBuf.writeByte(((PVUByte) f).get());                                     
                                    break;                                      
                                case pvUInt:
                                    System.out.println("uint");  
                                    byteBuf.writeInt(((PVUInt) f).get());                                     
                                    break;  
                                case pvULong:
                                    System.out.println("ul");  
                                    byteBuf.writeLong(((PVLong) f).get());                                     
                                    break;  
                                case pvUShort:
                                    System.out.println("us");                                    
                                    byteBuf.writeShort(((PVUShort) f).get());                                     
                                    break; 
                            }
                            
                            System.out.println(ByteBufUtil.prettyHexDump(byteBuf));
                            //if ByteBuf is not null we check the offets
                            
                            
                        };
                        
                        index = changedBitSet.nextSetBit(index);
                        
                    }
                    
                    
                }
                
//                if (changedBitSet.get(1) && 
//                    (changedBitSet.length() == 2) &&
//                    overrunBitSet.isEmpty()) {
//
//                    final DBRecord dbRecord = recordMonitors.get(monitor);
//                    final Optional<PlcItem> optPlcItem = dbRecord.getPlcItem();
//                    LOGGER.info(ByteBufUtil.prettyHexDump(dbRecord.getWriteBuffer().get()));
//                    
//                    if (optPlcItem.isPresent()) {
//                        optPlcItem.get().itemWrite(dbRecord.getWriteBuffer().get(), dbRecord.getByteOffset(), dbRecord.getBiteOffset());  
//                    }
//
//                }
            }
        } catch (Exception ex) {
             LOGGER.error(ex.getMessage());
             ex.printStackTrace();
        } finally {
            monitor.release(element);             
        }        
    }
    
      
    @Override
    public void unlisten(Monitor monitor) {
        recordMonitors.remove(monitor);
    }

    @Override
    public String getRequesterName() {
        return "getRequesterName()";
    }

    @Override
    public void message(String message, MessageType messageType) {
        
    }

    @Override
    public void putDBRecord(DBRecord dbRecord) {
        LOGGER.info("Monitor with fields =  {}", dbRecord.getFieldsToMonitor());
        PVStructure request = createRequest.createRequest(dbRecord.getFieldsToMonitor());
        Monitor monitor = MonitorFactory.create(dbRecord, this, request);
        if (null != monitor) {
            recordMonitors.put(monitor, dbRecord);
            monitor.start();
        } else {
            LOGGER.error("The monitor is 'null' for [{}]", dbRecord.getRecordName());
        }
    }

    @Override
    public void removeDBRecord(DBRecord dbRecord) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
