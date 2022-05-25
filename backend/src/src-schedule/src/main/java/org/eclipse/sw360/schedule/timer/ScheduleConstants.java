/*
 * Copyright Siemens AG, 2016. Part of the SW360 Portal Project.
 * With modifications from Bosch Software Innovations GmbH, 2016.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */

package org.eclipse.sw360.schedule.timer;


import org.eclipse.sw360.datahandler.common.CommonUtils;
import org.eclipse.sw360.datahandler.thrift.ThriftClients;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author stefan.jaeger@evosoft.com
 */
public class ScheduleConstants {
    private static final Logger log = LogManager.getLogger(ScheduleConstants.class);

    private ScheduleConstants(){}

    public static final String PROPERTIES_FILE_PATH = "/sw360.properties";
    public static final String CVESEARCH_OFFSET_PROPERTY_NAME = "schedule.cvesearch.firstOffset.seconds";
    public static final String CVESEARCH_INTERVAL_PROPERTY_NAME = "schedule.cvesearch.interval.seconds";
    public static final String AUTOSTART_PROPERTY_NAME = "autostart";
    public static final String CVESEARCH_OFFSET_DEFAULT  = 0 + "" ; // default 00:00 am, in seconds
    public static final String CVESEARCH_INTERVAL_DEFAULT  = (24*60*60)+"" ; // default 24h, in seconds
    public static final String DELETE_ATTACHMENT_OFFSET_DEFAULT  = "0"; // default 00:00 am, in seconds
    public static final String DELETE_ATTACHMENT_INTERVAL_DEFAULT  = (24*60*60) + "" ; // default 24h, in seconds
    public static final String DELETE_ATTACHMENT_OFFSET_PROPERTY_NAME = "schedule.delete.attachment.firstOffset.seconds";
    public static final String DELETE_ATTACHMENT_INTERVAL_PROPERTY_NAME = "schedule.delete.attachment.interval.seconds";
    public static final String DEPARTMENT_OFFSET_PROPERTY_NAME = "schedule.department.firstOffset.seconds";
    public static final String DEPARTMENT_INTERVAL_PROPERTY_NAME = "schedule.department.interval.seconds";
    public static final String DEPARTMENT_OFFSET_DEFAULT  = "0" ; // default 00:00 am, in seconds

    // scheduler properties
    public static final ConcurrentHashMap<String, Integer> SYNC_FIRST_RUN_OFFSET_SEC = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Integer> SYNC_INTERVAL_SEC = new ConcurrentHashMap<>();
    public static final String[] autostartServices;
    public static Set<String> invalidConfiguredServices = new HashSet<>();

    static {
        Properties props = CommonUtils.loadProperties(ScheduleConstants.class, PROPERTIES_FILE_PATH);

        if(! props.containsKey(CVESEARCH_OFFSET_PROPERTY_NAME)){
            log.info("Property "+ CVESEARCH_OFFSET_PROPERTY_NAME + " not set. Using default value.");
        }
        String cveSearchOffset  = props.getProperty(CVESEARCH_OFFSET_PROPERTY_NAME, CVESEARCH_OFFSET_DEFAULT);
        try {
            SYNC_FIRST_RUN_OFFSET_SEC.put(ThriftClients.CVESEARCH_SERVICE, Integer.parseInt(cveSearchOffset));
        } catch (NumberFormatException nfe){
            log.error("Property " + CVESEARCH_OFFSET_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.CVESEARCH_SERVICE);
        }

        if(! props.containsKey(CVESEARCH_INTERVAL_PROPERTY_NAME)){
            log.info("Property "+ CVESEARCH_INTERVAL_PROPERTY_NAME + " not set. Using default value.");
        }
        String cveSearchInterval  = props.getProperty(CVESEARCH_INTERVAL_PROPERTY_NAME, CVESEARCH_INTERVAL_DEFAULT);
        try {
            SYNC_INTERVAL_SEC.put(ThriftClients.CVESEARCH_SERVICE, Integer.parseInt(cveSearchInterval));
        } catch (NumberFormatException nfe){
            log.error("Property " + CVESEARCH_INTERVAL_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.CVESEARCH_SERVICE);
        }

        if(! props.containsKey(DELETE_ATTACHMENT_OFFSET_PROPERTY_NAME)){
            log.debug("Property " + DELETE_ATTACHMENT_OFFSET_PROPERTY_NAME + " not set. Using default value.");
        }
        String deleteAttachmentOffset  = props.getProperty(DELETE_ATTACHMENT_OFFSET_PROPERTY_NAME, DELETE_ATTACHMENT_OFFSET_DEFAULT);
        try {
            SYNC_FIRST_RUN_OFFSET_SEC.put(ThriftClients.DELETE_ATTACHMENT_SERVICE, Integer.parseInt(deleteAttachmentOffset));
        } catch (NumberFormatException nfe){
            log.error("Property " + DELETE_ATTACHMENT_OFFSET_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.DELETE_ATTACHMENT_SERVICE);
        }

        if(! props.containsKey(DELETE_ATTACHMENT_INTERVAL_PROPERTY_NAME)){
            log.debug("Property "+ DELETE_ATTACHMENT_INTERVAL_PROPERTY_NAME + " not set. Using default value.");
        }
        String deleteAttachmentInterval  = props.getProperty(DELETE_ATTACHMENT_INTERVAL_PROPERTY_NAME, DELETE_ATTACHMENT_INTERVAL_DEFAULT);
        try {
            SYNC_INTERVAL_SEC.put(ThriftClients.DELETE_ATTACHMENT_SERVICE, Integer.parseInt(deleteAttachmentInterval));
        } catch (NumberFormatException nfe){
            log.error("Property " + DELETE_ATTACHMENT_INTERVAL_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.DELETE_ATTACHMENT_SERVICE);
        }

        if(! props.containsKey(DEPARTMENT_OFFSET_PROPERTY_NAME)){
            log.info("Property "+ DEPARTMENT_OFFSET_PROPERTY_NAME + " not set. Using default value.");
        }
        String departmentOffset  = props.getProperty(DEPARTMENT_OFFSET_PROPERTY_NAME, DEPARTMENT_OFFSET_DEFAULT);
        try {
            SYNC_FIRST_RUN_OFFSET_SEC.put(ThriftClients.IMPORT_DEPARTMENT_SERVICE, Integer.parseInt(departmentOffset));
        } catch (NumberFormatException nfe){
            log.error("Property " + DEPARTMENT_OFFSET_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
        }
        if(! props.containsKey(DEPARTMENT_INTERVAL_PROPERTY_NAME)){
            log.info("Property "+ DEPARTMENT_INTERVAL_PROPERTY_NAME + " not set. Using default value.");
        }
        String departmentInterval  = props.getProperty(DEPARTMENT_INTERVAL_PROPERTY_NAME, DEPARTMENT_OFFSET_DEFAULT);
        try {
            SYNC_INTERVAL_SEC.put(ThriftClients.IMPORT_DEPARTMENT_SERVICE, Integer.parseInt(departmentInterval));
        } catch (NumberFormatException nfe){
            log.error("Property " + DEPARTMENT_INTERVAL_PROPERTY_NAME + " is not an integer.");
            invalidConfiguredServices.add(ThriftClients.IMPORT_DEPARTMENT_SERVICE);
        }

        String autostartServicesString = props.getProperty(AUTOSTART_PROPERTY_NAME, "");
        autostartServices = autostartServicesString.split(",");
    }
}
