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
package org.apache.plc4x.nifi;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.plc4x.nifi.util.Plc4xCommonTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Plc4xNifiTest {
    

    private static File dumyAddressesFile;

    public static File getDumyAddressesFile() {
        return dumyAddressesFile;
    }

    @BeforeAll
    public static void beforeAll() {

        dumyAddressesFile = new File("dumy-address-file");

        try (FileWriter fileWriter = new FileWriter(dumyAddressesFile)) {
            ObjectMapper mapper = new ObjectMapper();
            fileWriter.write(mapper.writeValueAsString(Plc4xCommonTest.getAddressMap()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @AfterAll
    public static void afterAll() {
        dumyAddressesFile.delete();
    }
}
