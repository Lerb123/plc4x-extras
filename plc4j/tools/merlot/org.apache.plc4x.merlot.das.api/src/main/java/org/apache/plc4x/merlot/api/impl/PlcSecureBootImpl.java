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
package org.apache.plc4x.merlot.api.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.apache.plc4x.java.api.PlcDriver;
import org.apache.plc4x.merlot.api.PlcGeneralFunction;
import org.apache.plc4x.merlot.api.PlcSecureBoot;
import org.apache.plc4x.merlot.scheduler.api.Job;
import org.apache.plc4x.merlot.scheduler.api.JobContext;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.LoggerFactory;


public class PlcSecureBootImpl implements PlcSecureBoot, Job {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(PlcSecureBootImpl.class);
    private static final String DB_URL = "jdbc:sqlite:data/boot.db";
        
    private static final String SQL_CREATE_TABLE_DEVICES = 
            "CREATE TABLE IF NOT EXISTS Devices("
            + "DeviceId INTEGER PRIMARY KEY,"
            + "DriverName TEXT,"            
            + "DeviceName TEXT,"
            + "DeviceId TEXT,"
            + "ShortName TEXT,"
            + "Description TEXT,"
            + "Md5 TEXT)";
    
    private static final String SQL_CREATE_TABLE_GROUPS = 
            "CREATE TABLE IF NOT EXISTS Groups("
            + "GroupId INTEGER PRIMARY KEY,"
            + "DeviceId INTEGER,"            
            + "GroupName TEXT,"
            + "GroupDescripcion TEXT,"
            + "GroupScantime TEXT,"
            + "Md5 TEXT)";  
    
    private static final String SQL_CREATE_TABLE_ITEMS = 
            "CREATE TABLE IF NOT EXISTS Items("
            + "ItemId INTEGER PRIMARY KEY,"
            + "DeviceId INTEGER,"
            + "GroupId INTEGER,"
            + "ItemName TEXT,"
            + "ItemDescripcion TEXT,"
            + "ItemTag TEXT,"
            + "Md5 TEXT)";  
    
    private static final String SQL_SELECT_DEVICES = 
            "SELECT * FROM Devices WHERE DriverName = ?";
    
    private static final String SQL_SELECT_GROUPS = 
            "SELECT * FROM Groups WHERE DeviceId = ?";  
    
    private static final String SQL_SELECT_ITEMS = 
            "SELECT * FROM  WHERE GroupId = ?";     
    
    
    private Map<String, PlcDriver> delayedBootPlcDivers = new ConcurrentHashMap<>();
    
    private final PlcGeneralFunction plcGeneralFunction;
    
    DataSourceFactory dsFactory = null;
    Connection dbConnection = null;

    public PlcSecureBootImpl(PlcGeneralFunction plcGeneralFunction) {
        this.plcGeneralFunction = plcGeneralFunction;
    }
        
    @Override
    public void init() {
        if (null != dsFactory) {
            Properties props = new Properties();
            props.setProperty(DataSourceFactory.JDBC_URL, DB_URL);

            try {
                DataSource ds = dsFactory.createDataSource(props);
                dbConnection = ds.getConnection();
                if (null != dbConnection) {
                    var databaseMetaData = dbConnection.getMetaData();
                    LOGGER.info("Boot driver name is {}.", databaseMetaData.getDriverName());
                    createTables();
                    //Catalog,Schema, Table pattern,types of tables
                    try(ResultSet resultSet = databaseMetaData.getTables(null, null, null, new String[]{"TABLE"})){ 
                      while(resultSet.next()) { 
                        String tableName = resultSet.getString("TABLE_NAME"); 
                        String remarks = resultSet.getString("REMARKS"); 
                      }
                    }                    
                    
                }
            } catch (SQLException ex) {
                LOGGER.info(ex.getMessage());
            }
        }
    }

    @Override
    public void destroy() {
        if (null != dbConnection) {
            try {
                dbConnection.close();
            } catch (SQLException ex) {
                LOGGER.info(ex.getMessage());
            }
        }    
    }

    @Override
    public void bindPlcDriver(PlcDriver plcDriver) {
        LOGGER.info("Loading driver: {}",plcDriver.getProtocolCode());
        if (null != dbConnection) {
            
        } else {
            LOGGER.info("Delayed start of driver {}.",plcDriver.getProtocolCode());
            delayedBootPlcDivers.put(plcDriver.getProtocolCode(), plcDriver);
        }
    }

    @Override
    public void unbindPlcDriver(PlcDriver plcDriver) {
        
    }
    
    @Override
    public void bindDataSourceFactory(DataSourceFactory dsFactory) {
        this.dsFactory = dsFactory;
        init();
    }  
    
   
    @Override
    public void execute(JobContext context) {
        if (null != dbConnection) {
            if (!delayedBootPlcDivers.isEmpty()) {
                Set<String> keys = delayedBootPlcDivers.keySet();
                for (String key:keys) {
                    delayedBootPlcDivers.remove(key);
                }
            }            
        }        
    }

    @Override
    public void persist() {
        var plcDrivers = plcGeneralFunction.getPlcDrivers();
        plcDrivers.forEach( (k, d) -> store(k));
    }

    @Override
    public void store(String plcDriver) {
        var plcDevices = plcGeneralFunction.getPlcDevices(plcDriver);
        plcDevices.forEach((duid, dname) ->{
            var plcDevice = plcGeneralFunction.getPlcDevice(duid);
            //Store Device
            var plcGroups = plcGeneralFunction.getPlcDeviceGroups(duid);
            plcGroups.forEach((guid, gname) -> {
                var plcGroup = plcGeneralFunction.getPlcGroup(guid);
                //Store Group
                var plcItems = plcGeneralFunction.getPlcGroupItems(guid);
                plcItems.forEach((iuid, iname) -> {
                    var plcItem = plcGeneralFunction.getPlcItem(iuid);
                    //Store item
                    
                });
            });
        
        });
    }
    
    @Override
    public void restore(String plcDriver) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    
    
    private void createTables() throws SQLException{
        Statement statement;
        statement = dbConnection.createStatement();
        
        statement.execute(SQL_CREATE_TABLE_DEVICES);
        statement.execute(SQL_CREATE_TABLE_GROUPS);        
        statement.execute(SQL_CREATE_TABLE_ITEMS);                 
    }
    
}
