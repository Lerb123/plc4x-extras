/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.apache.plc4x.merlot.isa182.alarm;

/**
 *
 * @author luis
 */
public interface MalbecManagerLogin {
    
    /**
     * 
     * @param username
     * @param password
     * @return 
     */
    boolean login(String username, String password);
    /**
     * 
     * @param username
     * @return 
     */
    boolean closeLogin(String username);
    /**
     * 
     * @param username
     * @return 
     */
    boolean isLogged(String username);
    /**
     * 
     * @param username
     * @return Grado de permisología
     */
    int checkPermissions(String username);
}
