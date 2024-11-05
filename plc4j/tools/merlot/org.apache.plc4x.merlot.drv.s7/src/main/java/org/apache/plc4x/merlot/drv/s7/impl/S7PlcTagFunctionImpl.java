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
package org.apache.plc4x.merlot.drv.s7.impl;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.plc4x.java.api.model.PlcTag;
import static org.apache.plc4x.java.api.types.PlcValueType.BOOL;
import static org.apache.plc4x.java.api.types.PlcValueType.USINT;
import org.apache.plc4x.java.s7.readwrite.MemoryArea;
import static org.apache.plc4x.java.s7.readwrite.MemoryArea.DATA_BLOCKS;
import static org.apache.plc4x.java.s7.readwrite.MemoryArea.DIRECT_PERIPHERAL_ACCESS;
import static org.apache.plc4x.java.s7.readwrite.MemoryArea.FLAGS_MARKERS;
import static org.apache.plc4x.java.s7.readwrite.MemoryArea.INPUTS;
import static org.apache.plc4x.java.s7.readwrite.MemoryArea.OUTPUTS;
import org.apache.plc4x.java.s7.readwrite.TransportSize;
import org.apache.plc4x.java.s7.readwrite.tag.S7Tag;
import org.apache.plc4x.merlot.api.PlcTagFunction;
import org.osgi.framework.BundleContext;
import org.osgi.service.dal.OperationMetadata;
import org.osgi.service.dal.PropertyMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class S7PlcTagFunctionImpl implements PlcTagFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(S7PlcTagFunctionImpl.class);
    private static final boolean PLC4X_TAG = true;    
    private BundleContext bc; 
    
          
    public S7PlcTagFunctionImpl(BundleContext bc) {
        this.bc = bc;
    }   
    
    //TODO: Remove since the S7Tag builder is public
    private ImmutablePair<PlcTag, Object[]> getStringPlcTag(PlcTag plcTag, ByteBuf byteBuf, int byteOffset, byte bitOffset) {
        LOGGER.info("PlcTag class {} and type {} ", plcTag.getClass(),  plcTag.getPlcValueType());
        short tempValue = 0;
        S7Tag s7PlcTag = null;        
        if (plcTag instanceof S7Tag){
            final S7Tag s7Tag = (S7Tag) plcTag;
            LOGGER.info("Processing S7Tag: {}", s7Tag.toString()); 
            Object[] objValues = new Object[byteBuf.capacity()];
            StringBuilder strTagBuilder = new StringBuilder();
            switch (s7Tag.getPlcValueType()) { 
                case BOOL:
                        switch (s7Tag.getMemoryArea()){
                            case DATA_BLOCKS:;
                                strTagBuilder.append("%DB").
                                    append(s7Tag.getBlockNumber()).
                                    append(".DBX").
                                    append(byteOffset).
                                    append(".").
                                    append(bitOffset).
                                    append(":").
                                    append(s7Tag.getDataType().name()).
                                    append("[").
                                    append(byteBuf.capacity()).
                                    append("]");
                                break;
                            case DIRECT_PERIPHERAL_ACCESS:
                            case INPUTS:
                            case OUTPUTS:
                            case FLAGS_MARKERS:
                                strTagBuilder.append("%").
                                    append(s7Tag.getMemoryArea().getShortName()).
                                    append(s7Tag.getDataType().getDataTransportSize()).
                                    append(byteOffset).
                                    append(".").
                                    append(bitOffset).
                                    append(":").
                                    append(s7Tag.getDataType().name()).
                                    append("[").
                                    append(byteBuf.capacity()).
                                    append("]");                                        
                                break;
                            default:; 
                        }
                        byteBuf.resetReaderIndex();
                        for (int i=0; i < byteBuf.capacity(); i++){
                            objValues[i] = byteBuf.readBoolean();
                        }                        
                    break;
                case UINT:  
                        byteOffset = s7Tag.getByteOffset() + byteOffset * byteBuf.capacity();                    
                        switch (s7Tag.getMemoryArea()){
                            case DATA_BLOCKS:;
                                strTagBuilder.append("%DB").
                                    append(s7Tag.getBlockNumber()).
                                    append(".DBB").
                                    append(byteOffset).
                                    append(":").
                                    append(s7Tag.getDataType().name()).
                                    append("[").
                                    append(byteBuf.capacity()).
                                    append("]");                           
                                break;
                            case DIRECT_PERIPHERAL_ACCESS:
                            case INPUTS:
                            case OUTPUTS:
                            case FLAGS_MARKERS:
                                strTagBuilder.append("%").
                                    append(s7Tag.getMemoryArea().getShortName()).
                                    append(s7Tag.getDataType().getDataTransportSize()).
                                    append(byteOffset).
                                    append(":").
                                    append(s7Tag.getDataType().name()).
                                    append("[").
                                    append(byteBuf.capacity()).
                                    append("]");                                        
                                break;                                
                            default:; 
                        }
                        byteBuf.resetReaderIndex();
                        for (int i=0; i < byteBuf.capacity(); i++){
                            tempValue = (short) (byteBuf.readByte() & 0xFF);                            
                            objValues[i] = tempValue;
                        }                                  
                    break;
                default:;
                
            }
            LOGGER.info("Writing tag : {}",strTagBuilder.toString() );
            s7PlcTag = S7Tag.of(strTagBuilder.toString());
            return new ImmutablePair<>(s7PlcTag , objValues);            
        }
        return null;
    }

    private ImmutablePair<PlcTag, Object[]> getPlc4xPlcTag(PlcTag plcTag, ByteBuf byteBuf, int byteOffset, byte bitOffset) {
        LOGGER.info("PlcTag class {} and type {} ", plcTag.getClass(),  plcTag.getPlcValueType());
        short tempValue = 0;
        int intBlockNumber = 0;
        int intByteOffset = 0;
        byte byBitOffset = 0;        
        S7Tag s7PlcTag = null;
        if (plcTag instanceof S7Tag){
            final S7Tag s7Tag = (S7Tag) plcTag;
            LOGGER.info("Processing S7Tag: {}", s7Tag.toString()); 
            LOGGER.info("Buffer: \r\n" + byteBuf.toString());
            Object[] objValues = new Object[byteBuf.readableBytes()];
            switch (s7Tag.getDataType()) { 
                case BYTE:  
                        intBlockNumber = (s7Tag.getMemoryArea() == MemoryArea.DATA_BLOCKS)?
                                            s7Tag.getBlockNumber() : 0;
                        intByteOffset = s7Tag.getByteOffset() + byteOffset;                         
                        s7PlcTag = new S7Tag(TransportSize.BOOL,
                                            s7Tag.getMemoryArea(),
                                            intBlockNumber,
                                            intByteOffset,
                                            bitOffset,
                                            byteBuf.capacity());
                        LOGGER.info("Write BOOL S7Tag: {}", s7PlcTag.toString());                         
                        byteBuf.resetReaderIndex();
                        for (int i=0; i < byteBuf.capacity(); i++){
                            objValues[i] = byteBuf.readBoolean();
                        }                        
                    break;
                case USINT:  
                        if (bitOffset == -1) {
                            intByteOffset = s7Tag.getByteOffset() + byteOffset;                    
                            s7PlcTag = new S7Tag(s7Tag.getDataType(),
                                                s7Tag.getMemoryArea(),
                                                s7Tag.getBlockNumber(),
                                                intByteOffset,
                                                (byte) 0,
                                                byteBuf.readableBytes());
                            LOGGER.info("Write ANY BYTES S7Tag: {}", s7PlcTag.toString()); 
                            byteBuf.resetReaderIndex();
                            int readableBytes =  byteBuf.readableBytes();
                            for (int i=0; i < readableBytes; i++){
                                tempValue = (short) (byteBuf.readByte() & 0xFF);                            
                                objValues[i] = tempValue;
                            }                              
                        } else {
                            intByteOffset = s7Tag.getByteOffset() + byteOffset;                    
                            s7PlcTag = new S7Tag(TransportSize.BOOL,
                                                s7Tag.getMemoryArea(),
                                                s7Tag.getBlockNumber(),
                                                intByteOffset,
                                                bitOffset,
                                                byteBuf.readableBytes());
                            LOGGER.info("Write ANY BOOL S7Tag: {}", s7PlcTag.toString());  
                            byteBuf.resetReaderIndex();
                            int readableBytes =  byteBuf.readableBytes();                            
                            for (int i=0; i < readableBytes; i++){
                                objValues[i] = byteBuf.readBoolean();
                            }                            
                        }
                                                                                
                    break;
                case COUNTER:                         
                        intByteOffset = ((s7Tag.getByteOffset() << 3) + 
                                         s7Tag.getBitOffset()) + (byteOffset / 2);                                                                   
                        s7PlcTag = new S7Tag(s7Tag.getDataType(),
                                            s7Tag.getMemoryArea(),
                                            s7Tag.getBlockNumber(),
                                            intByteOffset,
                                            byBitOffset,
                                            byteBuf.capacity() / 2);
                        LOGGER.info("> Write COUNTER S7Tag: {}", s7PlcTag.toString()); 
                        byteBuf.resetReaderIndex();
                        objValues = new Object[byteBuf.capacity() / 2];
                        for (int i=0; i < byteBuf.readableBytes() / 2; i++){
                            tempValue = (short) (byteBuf.readShort() & 0xFFFF);                            
                            objValues[i] = tempValue;
                        }                                  
                    break;                    
                    
                default:;
                
            }
            if (null != s7PlcTag)
                LOGGER.info("Writing tag : {}", s7PlcTag.toString());         
            return new ImmutablePair<>(s7PlcTag, objValues);            
        }        
        return null;
    }
 
    @Override
    public ImmutablePair<PlcTag, Object[]> getPlcTag(PlcTag plcTag, ByteBuf byteBuf, int byteOffset, byte bitOffset) {
        if (!PLC4X_TAG) {
            return getStringPlcTag(plcTag, byteBuf, byteOffset, bitOffset);
        } else {
            return getPlc4xPlcTag(plcTag, byteBuf, byteOffset, bitOffset);            
        }
    }    
    

    @Override
    public PropertyMetadata getPropertyMetadata(String propertyName) {
        throw new UnsupportedOperationException("Not supported yet."); 
    }

    @Override
    public OperationMetadata getOperationMetadata(String operationName) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getServiceProperty(String propKey) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String[] getServicePropertyKeys() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    
}
