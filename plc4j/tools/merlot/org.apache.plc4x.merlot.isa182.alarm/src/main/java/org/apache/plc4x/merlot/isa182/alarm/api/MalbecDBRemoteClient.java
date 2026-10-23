/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.apache.plc4x.merlot.isa182.alarm;

/**
 *
 * @author luis
 */
public interface MalbecDBRemoteClient {
    
    String getAllAlarmRecords();//7 dias a partir e ahora
    String getAllAlarmRecordsBy(String dateStart, String dateEnd);
    String getAllAlarmRecordsBy(String actualDate, int numberDays);
    String getAllAlarmRecordsBy(String dateStart, String dateEnd, String area);
}
