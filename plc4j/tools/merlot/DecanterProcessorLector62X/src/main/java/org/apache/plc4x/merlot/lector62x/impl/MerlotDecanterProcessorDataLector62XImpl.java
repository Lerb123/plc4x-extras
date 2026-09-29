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
package org.apache.plc4x.merlot.lector62x.impl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import javax.sql.DataSource;
import org.apache.plc4x.merlot.lector62x.api.MerlotDecanterProcessorDataLector62X;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MerlotDecanterProcessorDataLector62XImpl implements MerlotDecanterProcessorDataLector62X {

    private final static Logger LOGGER = LoggerFactory.getLogger(MerlotDecanterProcessorDataLector62XImpl.class);

    private DataSource dataSource;
    private BundleContext bc;

    private final static String MERLOT_LECTOR62X_EVENT_TOPIC = "decanter/collect/socket";
    private static final String TABLE_NAME_PROPERTY = "table.name";
    private static final String DIALECT_PROPERTY = "dialect";
    private static final String DATASOURCE_TARGET = "dataSource.target";

    private Map<String, String> connectionProperties = new HashMap();

    //Nota: Cadena que envia el plc: codguia ;numeropieza ;totalpiezas ;oficinaorigen ;oficinadestino;servicio;casillero;ruta;peso
    private final static String createTableQueryGenericTemplate
            = "CREATE TABLE IF NOT EXISTS TABLENAME(id BIGINT NOT NULL PRIMARY KEY, "
            + "codguia VARCHAR(40), numeropieza BIGINT, totalpiezas BIGINT, oficinaorigen BIGINT, "
            + "oficinadestino BIGINT, servicio BIGINT, casillero BIGINT,"
            + " ruta BIGINT, peso DOUBLE)";
   

    private final static String insertQueryTemplate
            = "INSERT INTO TABLENAME(id, codguia, numeropieza, totalpiezas, oficinaorigen, "
            + "oficinadestino, servicio, casillero, ruta, peso) VALUES(?,?,?,?,?,?,?,?,?,?)";

    private Random rd;

    public MerlotDecanterProcessorDataLector62XImpl(BundleContext bc) {
        this.bc = bc;
        rd = new Random();
    }

    @Override
    public void init() {
        //TODO: Buscar servicio de Datasource en el contexto
        rd = new Random();
    }

    @Override
    public void destroy() {
        //Cierra la conexion

        rd = null;
    }

    @Override
    public void saveData(String... data) {
        //Guarda los datos de la lectura de la camra en sqlite
    }

    @Override
    public void handleEvent(Event event) {
        LOGGER.info("Processing event data from lector62x, sending to persistence");

        String topic = event.getTopic();
        if (topic.equalsIgnoreCase(MERLOT_LECTOR62X_EVENT_TOPIC)) {
            String payloadEvent = (String) event.getProperty("payload");
            
            System.out.println(payloadEvent);
            if (!payloadEvent.contains("NoRead")) {
                String[] splitPayload = payloadEvent.split(";");
                
              
                long id = Math.abs(rd.nextLong());
                String codguia = splitPayload[0].replace("\u0002", "");
                long numeropieza = Long.parseLong(splitPayload[1]);
                long totalpiezas = Long.parseLong(splitPayload[2]);
                long oficinaorigen = Long.parseLong(splitPayload[3]);
                long oficinadestino = Long.parseLong(splitPayload[4]);
                long servicio = Long.parseLong(splitPayload[5]);
                long casillero = Long.parseLong(splitPayload[6]);
                long ruta = Long.parseLong(splitPayload[7].replace("\u0003", ""));
                
                //TODO: Formatear a dos decimales el peso.!
                float peso = Float.parseFloat(splitPayload[8].trim());
                
                try (Connection connection = dataSource.getConnection()) {
                    String insertQuery = insertQueryTemplate.replaceAll("TABLENAME", this.connectionProperties.get(TABLE_NAME_PROPERTY));
                    try (PreparedStatement insertStatement = connection.prepareStatement(insertQuery)) {
                        insertStatement.setLong(1, id);
                        insertStatement.setString(2, codguia);
                        insertStatement.setLong(3, numeropieza);
                        insertStatement.setLong(4, totalpiezas);
                        insertStatement.setLong(5, oficinaorigen);
                        insertStatement.setLong(6, oficinadestino);
                        insertStatement.setLong(7, servicio);
                        insertStatement.setLong(8, casillero);
                        insertStatement.setLong(9, ruta);
                        insertStatement.setFloat(10, peso);

                        //Submit the form
                        if (insertStatement.executeUpdate() > 1) {
                            LOGGER.info("Registro insertado con codigo de guia {}", codguia);
                        }
                    } catch (Exception e) {
                        LOGGER.info("Error inserting a record into the DataSource {}", this.connectionProperties.get(TABLE_NAME_PROPERTY));
                    }
                } catch (SQLException ex) {
                    LOGGER.info(ex.getMessage());
                }

            }
        }
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        if (properties == null || properties.isEmpty()) {
            return;
        }

        //Clean properties
        this.connectionProperties.clear();

        String tableName = (String) properties.get(TABLE_NAME_PROPERTY);

        if (tableName == null || tableName.trim().isEmpty()) {
            throw new ConfigurationException("table.name", "The ‘table.name’ property cannot be empty.");
        }

        String dialect = (String) properties.get(DIALECT_PROPERTY);

        if (dialect == null || dialect.trim().isEmpty()) {
            throw new ConfigurationException("dialect", "The ‘dialect’ property cannot be empty.");
        }

        String jndiName = (String) properties.get(DATASOURCE_TARGET);

        if (jndiName == null || jndiName.trim().isEmpty()) {
            throw new ConfigurationException("dataSource.target", "The ‘dataSource.target’ property cannot be empty.");
        }

        this.connectionProperties.put(TABLE_NAME_PROPERTY, tableName);
        this.connectionProperties.put(DIALECT_PROPERTY, dialect);
        this.connectionProperties.put(DATASOURCE_TARGET, jndiName);

        //----------------------End Validate Properties------------------------------------
        getDataSourceForPropertiesConnectionAndCreateTable();
    }

    private void getDataSourceForPropertiesConnectionAndCreateTable() throws ConfigurationException {
        //----------------------Get services Datasources----------------------------------
        try {
            ServiceReference[] refDataSource = bc.getAllServiceReferences(DataSource.class.getName(), this.connectionProperties.get(DATASOURCE_TARGET));

            this.dataSource = (DataSource) bc.getService(refDataSource[0]);

            //It only triggers the table creation if the data source is available
            if (this.dataSource.getConnection() != null) {
                getConnectionAndconstructTable();
            }

            LOGGER.info("DataSource found: {}", this.dataSource.getClass().getName());

        } catch (Exception e) {
            throw new ConfigurationException(null, "Error retrieving the DataSource", e);
        }
        //---------------------End get services-------------------------------------------
    }

    public void getConnectionAndconstructTable() {
        try (Connection connection = dataSource.getConnection()) {
            createTable(connection);
        } catch (Exception e) {
            LOGGER.info("Error creating table schemas: {}", e.getMessage());
        }
    }

    private void createTable(Connection connection) {
        String createTemplate = null;

        if (this.connectionProperties.get(DIALECT_PROPERTY).equals("generic")) {
            createTemplate = createTableQueryGenericTemplate;
        } else if (this.connectionProperties.get(DIALECT_PROPERTY).equals("oracle")) {
            //TODO
            LOGGER.info("Not Support this option");
        }

        String createTableQuery = createTemplate.replaceAll("TABLENAME", this.connectionProperties.get(TABLE_NAME_PROPERTY));

        try (Statement createStatement = connection.createStatement()) {
            createStatement.executeUpdate(createTableQuery);
            LOGGER.info("Table {} has been created", this.connectionProperties.get(TABLE_NAME_PROPERTY));
        } catch (SQLException e) {
            LOGGER.info("Can't create table {} \n {}", TABLE_NAME_PROPERTY, e.getMessage());
        }
    }
}
