package com.techsample.db.api.util;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

/**
 *
 * @author Meng Zhang
 */
public class DBUtil {
    private static MongoClientSettings DB_SETTINGS = null;
    private final static MongoClient DB_CLIENT = MongoClients.create(getDBSettings());
    private static final String DB_NAME = "aci_test";
    public static final String COURSES_COLLECTION_NAME = "courses";
    public static final String COUNTER_COLLECTION_NAME = "counter";
    protected final static String DEF_SKIP = "0";
    protected final static String DEF_LIMIT = Integer.MAX_VALUE + "";
    
    /**
     * Get MongoDB client setting, currently point to an Atlas instance
     * 
     * @return 
     */
    public static MongoClientSettings getDBSettings() {
        if (DB_SETTINGS == null) {
            String uri = "mongodb+srv://cources_api_access2:YjsneVWpvJKfsWx7@cluster0.upixo.mongodb.net/?retryWrites=true&w=majority";
            ConnectionString connectionString = new ConnectionString(uri);
            DB_SETTINGS = MongoClientSettings.builder().applyConnectionString(connectionString).build();
        }
        return DB_SETTINGS;
    }
    
    public static void InitializeCourseCollection() {
        // TODO create initial collections for test purpose
    }
    
    /**
     * Get default collection (course)
     * 
     * @return 
     */
    public static MongoCollection<Document> getCollection() {
        return getCollection(COURSES_COLLECTION_NAME);
    }
    
    /**
     * Get counter collection for auto-increase ID
     * 
     * @return 
     */
    public static MongoCollection<Document> getCounterCollection() {
        return getCollection(COUNTER_COLLECTION_NAME);
    }
    
    /**
     * Get collection with given collection name
     * 
     * @param collectionName
     * @return 
     */
    public static MongoCollection<Document> getCollection(String collectionName) {
        return DB_CLIENT.getDatabase(DB_NAME).getCollection(collectionName);
    }
}
