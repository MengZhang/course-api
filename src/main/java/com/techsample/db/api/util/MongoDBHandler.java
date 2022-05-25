package com.techsample.db.api.util;

import ch.qos.logback.classic.Logger;
import com.mongodb.Block;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import static com.mongodb.client.model.Updates.inc;
import static com.techsample.db.api.util.DBUtil.getCounterCollection;
import java.util.ArrayList;
import java.util.function.Consumer;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Meng Zhang
 */
public class MongoDBHandler {
    
    private static final Logger LOG = (Logger) LoggerFactory.getLogger(MongoDBHandler.class);

    /**
     * Cache all the records into a list
     * 
     * @param collection
     * @return 
     */
    public static ArrayList<Document> list(MongoCollection<Document> collection) {
        return list(collection, 0, Integer.MAX_VALUE);
    }
    
    /**
     * Cache all the records into a list and only includes
     * necessary data by given projection
     * 
     * @param collection
     * @param projection
     * @return 
     */
    public static ArrayList<Document> list(MongoCollection<Document> collection, Bson projection) {
        return list(collection, 0, Integer.MAX_VALUE, projection);
    }
    
    /**
     * Cache all the records within a range into a list 
     * 
     * @param collection
     * @param skip
     * @param limit
     * @return 
     */
    public static ArrayList<Document> list(MongoCollection<Document> collection, int skip, int limit) {

        ArrayList<Document> ret = new ArrayList();
        collection.find().skip(skip).limit(limit).forEach((Block<Document>) ret::add);
        return ret;

    }
    
    /**
     * Cache all the records within a range into a list and only includes
     * necessary data by given projection
     * 
     * @param collection
     * @param skip
     * @param limit
     * @param projection
     * @return 
     */
    public static ArrayList<Document> list(MongoCollection<Document> collection, int skip, int limit, Bson projection) {

        ArrayList<Document> ret = new ArrayList();
        collection.find().projection(projection).skip(skip).limit(limit).forEach((Consumer<Document>) ret::add);
        return ret;

    }
    
    /**
     * Find a record with ID object (string value)
     * 
     * @param collection
     * @param id
     * @return 
     */
    public static Document find(MongoCollection<Document> collection, String id) {
        return find(collection, new ObjectId(id));
    }
    
    
    /**
     * Find a record with ID object
     * 
     * @param collection
     * @param id
     * @return 
     */
    public static Document find(MongoCollection<Document> collection, ObjectId id) {
        return find(collection, eq("_id", id));
    }
    
    /**
     * Find a record with given search condition
     * 
     * @param collection
     * @param search
     * @return 
     */
    public static Document find(MongoCollection<Document> collection, Bson search) {
        return find(collection, search, null);
    }
    
    /**
     * Find a record with given search condition and necessary data by
     * given projection
     * 
     * @param collection
     * @param search
     * @param projection
     * @return 
     */
    public static Document find(MongoCollection<Document> collection, Bson search, Bson projection) {
        if (projection == null) {
            return collection.find(search).first();
        } else {
            return collection.find(search).projection(projection).first();
        }
    }
    
    public static ArrayList<Document> search(MongoCollection<Document> collection, Bson search) {
        return search(collection, search, 0, Integer.MAX_VALUE);
    }
    
    public static ArrayList<Document> search(MongoCollection<Document> collection, Bson search, int skip, int limit) {
        return search(collection, search, skip, limit, null);
    }
    
    public static ArrayList<Document> search(MongoCollection<Document> collection, Bson search, int skip, int limit, Bson projection) {
        ArrayList<Document> ret = new ArrayList<>();
        if (projection == null) {
            collection.find(search).skip(skip).limit(limit).forEach((Consumer<Document>) ret::add);
        } else {
            collection.find(search).projection(search).skip(skip).limit(limit).forEach((Consumer<Document>) ret::add);
        }
        return ret;
    }
    
    /**
     * Create new id for given collection
     * 
     * @param collectionName
     * @return 
     */
    public static int createNewId(String collectionName) {
        Document counter = update(getCounterCollection(), eq("_id", collectionName + "_id"), inc("sequence_value", 1));
        return counter.getInteger("sequence_value");
    }
    
    /**
     * Insert new record 
     * 
     * @param collection
     * @param record
     * @return 
     */
    public static boolean add(MongoCollection<Document> collection, Document record) {
        try {
            collection.insertOne(record);
            return true;
        } catch (MongoWriteException ex) {
            if(!ex.getMessage().contains("duplicate key")) {
                LOG.warn(ex.getMessage());
            }
            return false;
        }
    }
    
    public static boolean addSub(MongoCollection<Document> collection, Bson search, Bson record) {
        try {
            collection.findOneAndUpdate(search, record);
            return true;
        } catch (MongoWriteException ex) {
            LOG.warn(ex.getMessage());
            return false;
        }
    }
    
    public static <T> ArrayList<T> distinct(MongoCollection<Document> collection, String field, Class<T> type) {

        ArrayList<T> ret = new ArrayList();
        collection.distinct(field, type).forEach((Consumer<T>) ret::add);
        return ret;

    }
    
    public static Document update(MongoCollection<Document> collection, Bson search, Bson update) {
        return collection.findOneAndUpdate(search, update, new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER));
    }
}
