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
package org.apache.plc4x.merlot.db.impl;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.plc4x.merlot.api.DBWriterHandler;
import org.apache.plc4x.merlot.api.PlcGeneralFunction;
import org.apache.plc4x.merlot.api.PlcItem;
import org.apache.plc4x.merlot.api.PlcItemListener;
import org.apache.plc4x.merlot.api.PlcSecureBoot;
import org.apache.plc4x.merlot.api.TagSimplifier;
import org.apache.plc4x.merlot.api.core.MerlotProcessingDinamicsTags;
import org.apache.plc4x.merlot.api.core.MerlotProcessingDinamicsTags.Group;
import org.apache.plc4x.merlot.db.api.DBRecord;
import org.apache.plc4x.merlot.db.api.DBRecordFactory;
import org.epics.pvdata.pv.PVBoolean;
import org.epics.pvdata.pv.PVDouble;
import org.epics.pvdata.pv.PVScalar;
import org.epics.pvdata.pv.PVString;
import org.epics.pvdatabase.PVDatabase;
import org.epics.pvdatabase.PVRecord;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.jdbc.DataSourceFactory;
import org.slf4j.LoggerFactory;

public class DBPersistImpl implements EventHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(DBPersistImpl.class);
    private static final String DB_URL = "jdbc:sqlite:data/boot.db";

    private static final String SQL_CREATE_TABLE_PVRECORDS
            = "CREATE TABLE IF NOT EXISTS PvRecords("
            + "PvUuId TEXT NOT NULL PRIMARY KEY,"
            + "PvName TEXT,"
            + "PvType TEXT,"
            + "PvDeviceName TEXT," //Paso de PvId a PvDeviceName
            + "PvTag TEXT," //Paso de PvOffset a PvTag
            + "PvDescriptor TEXT,"
            + "PvScanTime TEXT,"
            + "PvScanEnable TEXT,"
            + "PvWriteEnable TEXT,"
            + "PvDisplayLimitLow TEXT,"
            + "PvDisplayLimitHigh TEXT,"
            + "PvDisplayDescription TEXT,"
            + "PvDisplayFormat TEXT,"
            + "PvDisplayUnits TEXT,"
            + "PvControlLimitLow TEXT,"
            + "PvControlLimitHigh TEXT,"
            + "PvControlMinStep TEXT,"
            + "Md5 TEXT)";

    private static final String SQL_SELECT_PVRECORDS
            = "SELECT * FROM PvRecords";

    private static final String SQL_INSERT_PVRECORDS
            = "INSERT INTO PvRecords(PvUuId, PvName, PvType, PvId, PvOffset, PvDescriptor, PvScanTime, pvScanEnable,"
            + "PvWriteEnable, PvDisplayLimitLow, PvDisplayLimitHigh, PvDisplayDescription, PvDisplayFormat,"
            + "PvDisplayUnits, PvControlLimitLow, PvControlLimitHigh, PvControlMinStep, Md5)"
            + "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
            + "ON CONFLICT(PvUuId) "
            + "DO "
            + "UPDATE SET "
            + "PvName =         excluded.PvName, "
            + "PvType =         excluded.PvType, "
            + "PvId =           excluded.PvId, "
            + "PvOffset =       excluded.PvOffset, "
            + "PvDescriptor =   excluded.PvDescriptor, "
            + "PvScanTime =     excluded.PvScanTime, "
            + "PvScanEnable =   excluded.PvScanEnable, "
            + "PvWriteEnable =  excluded.PvWriteEnable, "
            + "PvDisplayLimitLow =      excluded.PvDisplayLimitLow, "
            + "PvDisplayLimitHigh =     excluded.PvDisplayLimitHigh, "
            + "PvDisplayDescription =   excluded.PvDisplayDescription, "
            + "PvDisplayFormat =        excluded.PvDisplayFormat, "
            + "PvDisplayUnits =         excluded.PvDisplayUnits, "
            + "PvControlLimitLow =      excluded.PvControlLimitLow, "
            + "PvControlLimitHigh =     excluded.PvControlLimitHigh, "
            + "PvControlMinStep =       excluded.PvControlMinStep, "
            + "Md5 = excluded.Md5;";

    private final BundleContext bc;
    private final PVDatabase master;
    private final PlcGeneralFunction plcGeneralFunction;
//    private final DBWriterHandler writerHandler;
    DataSourceFactory dsFactory = null;
    Connection dbConnection = null;

    public DBPersistImpl(BundleContext bc,
            PVDatabase master,
            PlcGeneralFunction plcGeneralFunction
    ) {//DBWriterHandler writerHandler
        this.bc = bc;
        this.master = master;
        this.plcGeneralFunction = plcGeneralFunction;
//        this.writerHandler = writerHandler;
    }

    public void init() {
        LOGGER.info("INIT");
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
                    try (ResultSet resultSet = databaseMetaData.getTables(null, null, null, new String[]{"TABLE"})) {
                        while (resultSet.next()) {
                            String tableName = resultSet.getString("TABLE_NAME");
                            String remarks = resultSet.getString("REMARKS");
                        }
                    }
                    dbConnection.commit();
                    dbConnection.close();
                }
            } catch (SQLException ex) {
                LOGGER.info(ex.getMessage());
            }
        }
    }

    public void destroy() {
        LOGGER.info("DESTROY");
    }

    public void bindDataSourceFactory(DataSourceFactory dsFactory) {
        this.dsFactory = dsFactory;
        init();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(PlcSecureBoot.EVENT_STORE)) {
            try {
                store();
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
            }
        } else if (event.getTopic().equals(PlcSecureBoot.EVENT_RESTORE)) {
            try {
                restore();
            } catch (Exception ex) {
                LOGGER.error(ex.getMessage());
            }
        }
    }

    private void createTables() throws SQLException {
        Statement statement;
        statement = dbConnection.createStatement();
        statement.execute(SQL_CREATE_TABLE_PVRECORDS);
    }

    public void store() throws SQLException {
        String[] pvNames = master.getRecordNames();
        for (String pvName : pvNames) {
            final PVRecord pvRecord = master.findRecord(pvName);
            insertPvRecord(pvRecord);
        }
    }

    private record GroupKey(String deviceName, String scanTime) {}

    private static final class CumulativeGroupAndTag {

        boolean enabled;
        final List<TagSimplifier.TagRequest> requests = new ArrayList<>();   // tag + tipo de dato (PvType)
        final List<PVRecord> records = new ArrayList<>();
        final List<String> dbFactories = new ArrayList<>();

    }

    public void restore() throws SQLException, InvalidSyntaxException {
        if (dbConnection == null) {
            return;
        }


        Map<GroupKey, CumulativeGroupAndTag> tagsAndAccumulatedGroups = new LinkedHashMap<>();
        String filter = null;
        try (var stmt = dbConnection.createStatement(); var rs = stmt.executeQuery(SQL_SELECT_PVRECORDS)) {

            while (rs.next()) {
                String pvType = rs.getString("pvType");

                filter = "(db.record.type=" + pvType + ")";
                ServiceReference[] refs = bc.getServiceReferences(DBRecordFactory.class.getName(), filter);
                if (refs == null) {
                    // Se salta ANTES de agregar a las tres listas, para que sigan alineadas
                    LOGGER.warn("There is no DBRecordFactory for the type [{}], record [{}] skipped",
                            pvType, rs.getString("PvName"));
                    continue;
                }
                final DBRecordFactory recordFactory = (DBRecordFactory) bc.getService(refs[0]);
                String dbRecordDriver = (String) refs[0].getProperty("db.record.driver");
                PVRecord pvRecord = recordFactory.create(rs.getString("PvName"));

                
                //pvRecord.getPVStructure().getStringField("id").put(rs.getString("PvId"));
                //pvRecord.getPVStructure().getStringField("offset").put(rs.getString("PvOffset"));
                
                //Replace with PvDeviceName
                pvRecord.getPVStructure().getStringField("id").put(rs.getString("PvDeviceName"));
                //I use this field to generate the group
                pvRecord.getPVStructure().getStringField("scan_time").put(rs.getString("pvScanTime"));
                //It is set to 0 until it is recalculated.
                pvRecord.getPVStructure().getStringField("offset").put("0");

                pvRecord.getPVStructure().getStringField("descriptor").put(rs.getString("PvDescriptor"));
                pvRecord.getPVStructure().getBooleanField("scan_enable").put(Boolean.parseBoolean(rs.getString("PvScanEnable")));
                pvRecord.getPVStructure().getBooleanField("write_enable").put(Boolean.parseBoolean(rs.getString("PvWriteEnable")));
                pvRecord.getPVStructure().getDoubleField("display.limitLow").put(Double.parseDouble(rs.getString("PvDisplayLimitLow")));
                pvRecord.getPVStructure().getDoubleField("display.limitHigh").put(Double.parseDouble(rs.getString("PvDisplayLimitHigh")));
                pvRecord.getPVStructure().getStringField("display.description").put(rs.getString("PvDisplayDescription"));
                pvRecord.getPVStructure().getStringField("display.format").put(rs.getString("PvDisplayFormat"));
                pvRecord.getPVStructure().getStringField("display.units").put(rs.getString("PvDisplayUnits"));
                pvRecord.getPVStructure().getDoubleField("control.limitLow").put(Double.parseDouble(rs.getString("PvControlLimitLow")));
                pvRecord.getPVStructure().getDoubleField("control.limitHigh").put(Double.parseDouble(rs.getString("PvControlLimitHigh")));
                pvRecord.getPVStructure().getDoubleField("control.minStep").put(Double.parseDouble(rs.getString("PvControlMinStep")));

                //Aggregate by group (device + scan time)
                GroupKey clave = new GroupKey(rs.getString("PvDeviceName"), rs.getString("pvScanTime"));
                CumulativeGroupAndTag a = tagsAndAccumulatedGroups.computeIfAbsent(clave, k -> new CumulativeGroupAndTag());
                a.enabled |= Boolean.parseBoolean(rs.getString("PvScanEnable"));
                a.requests.add(new TagSimplifier.TagRequest(rs.getString("PvTag"), pvType));
                a.records.add(pvRecord);
                a.dbFactories.add(dbRecordDriver);
            }
        }
        createGrupsAndItems(tagsAndAccumulatedGroups);
        MerlotProcessingDinamicsTags.clear();
    }

    /**
     * 
     * @param pvRecord Process variable
     * @param plcItem Memory area to be linked to the process variable
     * @param dbFactory Define the data type that will be used to locate the specific write handler.
     */
    private void saveAndActiveWriter(PVRecord pvRecord, PlcItem plcItem, String dbFactory) {
        if (null == master.findRecord(pvRecord.getRecordName())) {
            //Attach the pvRecord to the item
            plcItem.addItemListener((PlcItemListener) pvRecord);
            //Register the pvRecord on the EPICS server
            master.addRecord(pvRecord);
            //The write handler type is assigned to that pvRecord
            Optional<DBWriterHandler> writer = plcGeneralFunction.getWriterHandler(dbFactory);
            if (writer.isPresent()) {
                writer.get().putDBRecord((DBRecord) pvRecord);
            }
        }

    }

    /**
     * 
     * @param tagsAndAccumulatedGroups List of: process variable addresses, process variables, and data type definitions
     */
    private void createGrupsAndItems(Map<GroupKey, CumulativeGroupAndTag> tagsAndAccumulatedGroups) {
        for (Map.Entry<GroupKey, CumulativeGroupAndTag> e : tagsAndAccumulatedGroups.entrySet()) {
            GroupKey key = e.getKey();
            CumulativeGroupAndTag a = e.getValue();

            Group g = MerlotProcessingDinamicsTags.getOrCreateGroup(
                    key.deviceName(), key.scanTime(), String.valueOf(a.enabled), plcGeneralFunction);
            if (g == null) {
                continue;
            }

            String driverCode = MerlotProcessingDinamicsTags.getDriverCodeByDeviceName(key.deviceName());

            try {
                ServiceReference[] refs = bc.getServiceReferences(TagSimplifier.class.getName(),
                        "(driver.code.tag.simplifier=" + driverCode + ")");
                if (refs == null) {
                    LOGGER.error("There is no TagSimplifier registered for the driver [{}]", driverCode);
                    continue;
                }

                TagSimplifier simplifier = (TagSimplifier) bc.getService(refs[0]);
                MerlotProcessingDinamicsTags.registerSimplifier(driverCode, simplifier);

                // Marca qué registros quedaron asociados a un item
                boolean[] covered = new boolean[a.records.size()];

                for (TagSimplifier.ItemBlock b : simplifier.simplify(a.requests, 15)) {
                    PlcItem item = MerlotProcessingDinamicsTags.setItem(g, b, plcGeneralFunction);
                    if (item == null) {
                        LOGGER.error("The item could not be created [{}]", b.tagSimplificado());
                        continue;
                    }

                    for (int i = 0; i < b.tagsOriginales().size(); i++) {
                        // Posición real del tag en a.requests, y por tanto en a.records y a.dbFactories
                        int src = b.sourceIndexes().get(i);
                        PVRecord pv = a.records.get(src);

                        // TODO: El offset se fija antes de registrar el record en el servidor
                        pv.getPVStructure().getStringField("offset").put(b.offsetRelativo(i).toString());
                        saveAndActiveWriter(pv, item, a.dbFactories.get(src));
                        covered[src] = true;
                    }
                }

                // Registros que no entraron en ningún bloque: tag inválido o tipo desconocido
                for (int k = 0; k < covered.length; k++) {
                    if (!covered[k]) {
                        LOGGER.info("PV [{}] has no item: invalid tag or unknown type [{}]",
                                a.records.get(k).getRecordName(), a.requests.get(k));
                    }
                }

            } catch (InvalidSyntaxException ex) {
                LOGGER.error("Error: {}", ex.getMessage());
            }
        }
    }

    private void insertPvRecord(PVRecord pvRecord) throws SQLException {
        if (null != dbConnection) {
            var query = dbConnection.prepareStatement(SQL_INSERT_PVRECORDS);

            if (pvRecord.getRecordName().contains("_")) {
                return;
            }

            PVScalar value = (PVScalar) pvRecord.getPVStructure().getSubField("value");

            query.setString(1, UUID.randomUUID().toString());
            query.setString(2, pvRecord.getRecordName());
            query.setString(3, value.getScalar().getScalarType().toString());

            query.setString(4, ((PVString) pvRecord.getPVStructure().getSubField("id")).get());
            query.setString(5, ((PVString) pvRecord.getPVStructure().getSubField("offset")).get());
            query.setString(6, ((PVString) pvRecord.getPVStructure().getSubField("descriptor")).get());
            query.setString(7, ((PVString) pvRecord.getPVStructure().getSubField("scan_time")).get());
            query.setString(8, Boolean.toString(((PVBoolean) pvRecord.getPVStructure().getSubField("scan_enable")).get()));
            query.setString(9, Boolean.toString(((PVBoolean) pvRecord.getPVStructure().getSubField("write_enable")).get()));
            query.setString(10, Double.toString(((PVDouble) pvRecord.getPVStructure().getSubField("display.limitLow")).get()));
            query.setString(11, Double.toString(((PVDouble) pvRecord.getPVStructure().getSubField("display.limitHigh")).get()));
            query.setString(12, ((PVString) pvRecord.getPVStructure().getSubField("display.description")).get());
            query.setString(13, ((PVString) pvRecord.getPVStructure().getSubField("display.format")).get());
            query.setString(14, ((PVString) pvRecord.getPVStructure().getSubField("display.units")).get());
            query.setString(15, Double.toString(((PVDouble) pvRecord.getPVStructure().getSubField("control.limitLow")).get()));
            query.setString(16, Double.toString(((PVDouble) pvRecord.getPVStructure().getSubField("control.limitHigh")).get()));
            query.setString(17, Double.toString(((PVDouble) pvRecord.getPVStructure().getSubField("control.minStep")).get()));
            query.setString(18, "");
            query.executeUpdate();
        }
    }

}