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
package org.apache.plc4x.merlot.logrecorder.core;

import java.io.File;
<<<<<<< HEAD
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
=======
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
>>>>>>> 169fec95 (General Changes Following the Migration to Version 1.0.0 of plc4x)
import org.osgi.framework.BundleContext;

public class MerlotLogRecorderFileExplorer {

<<<<<<< HEAD
    private final static String MERLOT_STORAGE_DIR = "MERLOT_STORAGE_DIR";
=======
    private final static String MERLOT_DATA_DIRECTORY = "karaf.data";
>>>>>>> 169fec95 (General Changes Following the Migration to Version 1.0.0 of plc4x)

    private MerlotLogRecorderFileExplorer() {
    }

<<<<<<< HEAD
    public static File findFileByFilename(String searchTerm) {
        
        String merlotStorageDir = System.getenv(MERLOT_STORAGE_DIR);
        //Search the data/tmp directory in Karaf
        Path fileTarget = Paths.get(merlotStorageDir, searchTerm);
=======
    public static File findFileByFilename(String searchTerm, BundleContext ctx) {
        
        String karafDataDir = ctx.getProperty(MERLOT_DATA_DIRECTORY);
        //Search the data/tmp directory in Karaf
        Path fileTarget = Paths.get(karafDataDir, "tmp", searchTerm);
>>>>>>> 169fec95 (General Changes Following the Migration to Version 1.0.0 of plc4x)

        if (Files.exists(fileTarget) && Files.isRegularFile(fileTarget)) {
            return fileTarget.toFile();
        }
        return null;

    }
}
