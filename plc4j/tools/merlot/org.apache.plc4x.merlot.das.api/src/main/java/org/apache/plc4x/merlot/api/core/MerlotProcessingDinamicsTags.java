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
package org.apache.plc4x.merlot.api.core;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.plc4x.merlot.api.PlcGeneralFunction;
import org.apache.plc4x.merlot.api.PlcItem;
import org.apache.plc4x.merlot.api.TagSimplifier;
import org.apache.plc4x.merlot.api.TagSimplifier.ItemBlock;

/**
 * Common to all drivers: devices, groups, and items. The simplification of tags
 * and the calculation of offsets are handled by the TagSimplifier registered
 * for each device's driver.
 */
public class MerlotProcessingDinamicsTags {

    private static final Map<String, Device> devices = new LinkedHashMap<>();
    private static final Map<String, Group> groups = new LinkedHashMap<>();
    private static final Map<String, Item> items = new LinkedHashMap<>();
    private static final Map<String, ItemBlock> itemBlock = new HashMap<>();
    private static final Map<ClaveGrupoIdx, Group> groupsByDeviceAndScan = new HashMap<>();

    //One simplifier per driver. Concurrent because each module registers it at its own time.
    private static final Map<String, TagSimplifier> simplifiers = new ConcurrentHashMap<>();

    private static int countGroup = 0;
    private static int countItem = 0;

    /**
     * Model a PlcDevice
     */
    public record Device(
            String deviceUuid,
            String driverName,
            String deviceKey,
            String deviceUrl,
            String deviceName,
            String deviceDescription,
            String deviceEnable) {}

     /**
     * Model a PlcGroup
     */
    public record Group(
            String groupUuid,
            String deviceUuid,
            String groupName,
            String groupDescription,
            String groupScanTime,
            String groupEnable) {}

     /**
     * Model a PlcItem
     */
    public record Item(
            String itemUuid,
            String groupUuid,
            String deviceUuid,
            String itemName,
            String itemDescription,
            String itemTag,
            String itemEnable) {}

    private record ClaveGrupoIdx(String deviceUuid, String scanTime) {}


    /* 
     * 
     * @param driverName Attached is the legend for the map showing the registered simplifiers
     * @param simplifier Simplification result obtained from the search
     */
    public static void registerSimplifier(String driverName, TagSimplifier simplifier) {
        simplifiers.put(normalizar(driverName), simplifier);
    }

    /**
     *
     * @param driverName Allows you to locate a registered simplifier on the
     * simplifier map using the driver code
     * @return Simplifier obtained through filtering
     */
    public static TagSimplifier getSimplifier(String driverName) {
        return simplifiers.get(normalizar(driverName));
    }

    /**
     *
     * @param s Remove whitespace and convert a text string to lowercase
     * @return A string with no leading or trailing whitespace and in lowercase
     */
    private static String normalizar(String s) {
        return s == null ? "" : s.trim().toLowerCase(Locale.ROOT);
    }

   

    /**
     *
     * @param deviceNameToSearchFor Name of the device that is linked to the
     * process variable
     * @return Driver code used to register the device
     */
    public static String getDriverCodeByDeviceName(String deviceNameToSearchFor) {
        return devices.values().stream()
                .filter(d -> deviceNameToSearchFor.equals(d.deviceName()))
                .map(Device::driverName)
                .findFirst()
                .orElse(null);
    }

    /**
     *
     * @param rsDevices Object retrieved from a query on the Devices table
     */
    public static void setDevice(ResultSet rsDevices) {
        try {
            Device d = new Device(
                    rsDevices.getString("DeviceUuid"),
                    rsDevices.getString("DriverName"),
                    rsDevices.getString("DeviceKey"),
                    rsDevices.getString("DeviceUrl"),
                    rsDevices.getString("DeviceName"),
                    rsDevices.getString("DeviceDescription"),
                    rsDevices.getString("DeviceEnable"));
            devices.put(d.deviceUuid(), d);
        } catch (SQLException ex) {
            System.out.println("Error: " + ex.getMessage());
        }
    }

    /**
     *
     * @param deviceNameToSearchFor Name of the device that is linked to the
     * process variable
     * @return The UUID used to register the device
     */
    public static String getDeviceByNameAndReturnUuid(String deviceNameToSearchFor) {
        return devices.values().stream()
                .filter(d -> deviceNameToSearchFor.equals(d.deviceName()))
                .map(Device::deviceUuid)
                .findFirst()
                .orElse(null);
    }

    /**
     *
     * @param deviceName Name of the device that is linked to the process
     * variable
     * @param timeScan Scan time for reading the variable, which is used to
     * generate the scan groups
     * @param enable Group Setup
     * @param gf An object that allows the group to be registered in the OSGi
     * service context.
     * @return A structured group
     */
    public static Group getOrCreateGroup(String deviceName, String timeScan, String enable, PlcGeneralFunction gf) {
        String uuidDevice = getDeviceByNameAndReturnUuid(deviceName);
        if (uuidDevice == null) {
            System.out.println("Device not found: " + deviceName);
            return null;
        }

        //If a group for that device with that scan time already exists, it is reused
        ClaveGrupoIdx clave = new ClaveGrupoIdx(uuidDevice, timeScan);
        Group existing = groupsByDeviceAndScan.get(clave);
        if (existing != null) {
            return existing;
        }

        int nCount = countGroup + 1;
        Group g = new Group(
                UUID.randomUUID().toString(),
                uuidDevice,
                String.format("GROUP00%s", nCount),
                String.format("Group that allows scanning every %s ms", timeScan),
                timeScan,
                enable);

        gf.createGroup(
                g.groupUuid(),
                g.deviceUuid(),
                g.groupName(),
                g.groupDescription(),
                g.groupScanTime(),
                g.groupEnable());

        countGroup = nCount;
        groups.put(g.groupUuid(), g);
        groupsByDeviceAndScan.put(clave, g);
        return g;
    }

    /**
     * A group of devices with a given scan time. null if none exist.
     *
     * @param deviceUuid UUID of the device to retrieve
     * @param scanTime Name of the registered device
     * @return Filtered group
     */
    public static Group getGroup(String deviceUuid, String scanTime) {
        return groupsByDeviceAndScan.get(new ClaveGrupoIdx(deviceUuid, scanTime));
    }

    /**
     * Create one item for each simplified block in the group.
     *
     * @param g Group that defines the scan time for the memory area block.
     * @param block List of blocks containing the list of original addresses,
     * the simplified addresses, offsets, area number, starting byte, and number
     * of bytes
     * @param gf An object that allows the item to be registered in the OSGi
     * service context.
     * @return PlcItem to link it to the PVRecords
     */
    public static PlcItem setItem(Group g, ItemBlock block, PlcGeneralFunction gf) {

        Item i = new Item(
                UUID.randomUUID().toString(),
                g.groupUuid(),
                g.deviceUuid(),
                String.format("ITEM00%s", ++countItem),
                String.format("Block of %d tags", block.tagsOriginales().size()),
                block.tagSimplificado(),
                g.groupEnable());

        items.put(i.itemUuid(), i);
        itemBlock.put(i.itemUuid(), block);

        Optional<PlcItem> optPlcItem = gf.createItem(i.itemUuid, i.groupUuid, i.deviceUuid, i.itemName, i.itemDescription, i.itemTag, i.itemEnable);
        System.out.println("Item created: " + i.itemName + " with simplified tags: " + i.itemTag);

        if (optPlcItem.isPresent()) {
            return optPlcItem.get();
        }
        return null;
    }

    /**
     * Block (with original tags and offsets) from which an item was derived.
     */
    /**
     *
     * @param itemUuid UUID of the registered block representing the memory area
     * of the PlcItem
     * @return Block that condenses the simplified tag for the PlcItem
     */
    public static ItemBlock getBlockItem(String itemUuid) {
        return itemBlock.get(itemUuid);
    }

    /**
     * For reloads: leave everything as it was at the beginning (registered
     * simplifiers are retained)
     */
    public static void clear() {
        devices.clear();
        groups.clear();
        groupsByDeviceAndScan.clear();
        items.clear();
        itemBlock.clear();
        countGroup = 0;
        countItem = 0;
    }
}
