package com.techsample.db.api.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author Meng Zhang
 */
public class DataUtil {
    
    private static JSONParser PARSER = new JSONParser();
    
    public static String getCurrrentTime() {
        LocalDateTime ldt = LocalDateTime.now(); // Create a date time object
        return ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'hh:mm:ss'Z'"));
    }
    
    public static JSONObject readJsonStr(String json) {
        try {
            return (JSONObject) PARSER.parse(json);
        } catch(Exception ex) {
            return null;
        }
        
    }
}
