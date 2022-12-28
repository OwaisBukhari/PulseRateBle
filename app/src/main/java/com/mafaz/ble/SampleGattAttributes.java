package com.mafaz.ble;


import java.util.HashMap;

/**
 * Created by brijesh on 15/4/17.
 */

public class SampleGattAttributes {
   // public static final String UUID_BATTERY_SERVICE = "0000ffc6-0000-1000-8000-00805f9b34fb";
    //public static final String UUID_BATTERY_LEVEL_UUID = "0000ffeb-0000-1000-8000-00805f9b34fb";

    public static final String UUID_BATTERY_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_BATTERY_LEVEL_UUID = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

    private static HashMap<String, String> attributes = new HashMap();

    static {
        attributes.put(UUID_BATTERY_LEVEL_UUID, "Connected");
        attributes.put(UUID_BATTERY_SERVICE, "Battery Service");
    }

    public static String lookup(String uuid) {
        String name = attributes.get(uuid);
        return name;
    }
}

