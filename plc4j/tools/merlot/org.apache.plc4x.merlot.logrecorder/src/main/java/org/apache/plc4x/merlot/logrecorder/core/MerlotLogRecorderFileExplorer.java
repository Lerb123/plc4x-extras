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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.osgi.framework.BundleContext;

public class MerlotLogRecorderFileExplorer {

    private final static String MERLOT_STORAGE_DIR = "MERLOT_STORAGE_DIR";

    private MerlotLogRecorderFileExplorer() {
    }

    public static File findFileByFilename(String searchTerm) {
        
        String merlotStorageDir = System.getenv(MERLOT_STORAGE_DIR);
        //Search the data/tmp directory in Karaf
        Path fileTarget = Paths.get(merlotStorageDir, searchTerm);

        if (Files.exists(fileTarget) && Files.isRegularFile(fileTarget)) {
            return fileTarget.toFile();
        }
        return null;

    }
}
