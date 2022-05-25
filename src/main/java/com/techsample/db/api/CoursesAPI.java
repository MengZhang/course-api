package com.techsample.db.api;

import ch.qos.logback.classic.Logger;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Projections.computed;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.combine;
import static com.mongodb.client.model.Updates.set;
import com.techsample.db.api.util.DBUtil;
import com.techsample.db.api.util.DataUtil;
import static com.techsample.db.api.util.MongoDBHandler.add;
import static com.techsample.db.api.util.MongoDBHandler.createNewId;
import static com.techsample.db.api.util.MongoDBHandler.list;
import static com.techsample.db.api.util.MongoDBHandler.find;
import static com.techsample.db.api.util.MongoDBHandler.update;
import java.net.URI;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.FormParam;
import javax.ws.rs.PUT;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.apache.commons.lang3.EnumUtils;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.slf4j.LoggerFactory;

/**
 * Course related path configuration class
 * 
 * @author Meng Zhang
 */
@Path("courses")
public class CoursesAPI {
    
    private final static Logger LOG = (Logger) LoggerFactory.getLogger(CoursesAPI.class);
    
    public static enum CourseStatus {
        scheduled,
        in_production,
        available;

        /**
         * Check if given string is a valid status value
         * @param val
         * @return
         */
        public static boolean isValid(String val) {
            return EnumUtils.isValidEnum(CourseStatus.class, val);
        }
    }
    
    /**
     * Configure path for GET /courses .
     * Returns a list of non-deleted courses, sorted by when they were created
     * with the oldest ones first.
     * 
     * @return 
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listCourses() {
        JSONObject ret = new JSONObject();
        try {
            ret.put("courses", list(DBUtil.getCollection(), fields(include("id", "name"), excludeId())));
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
        return Response.ok(ret.toJSONString()).build();
    }
    /**
     * Configure path for GET /courses/:id . 
     * If the course does not exist, return an HTTP 404 Not Found.
     * If the course is deleted, return an HTTP 410 Gone.
     * Return an HTTP 200 OK.
     * 
     * @param id
     * @return 
     */
    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response findCourse(@PathParam("id") int id) {
        try {
            Document ret = find(DBUtil.getCollection(), eq("id", id), fields(
                    include("id", "name", "status", "updatedAt", "deletedAt"),
                    computed("created_at", new Document("$getField", "createdAt")),
                    excludeId()));
            
            if (ret == null) {
                return Response.status(Status.NOT_FOUND).build();
            } else if (ret.get("deletedAt") != null){
                return Response.status(Status.GONE).build();
            } else {
                return Response.ok(ret.toJson()).build();
            }
            
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Create a new course with given name and status in JSON
     * 
     * @param jsonRequest
     * @return 
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createCourse(String jsonRequest) {
        JSONObject jsonObj = DataUtil.readJsonStr(jsonRequest);
        if (jsonObj == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        String name = (String) jsonObj.get("name");
        String status = (String) jsonObj.get("status");
        // validate input params
        if (name == null || "".equals(name.trim()) || status == null || !CourseStatus.isValid(status)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        // update database
        try {
            String timeStamp = DataUtil.getCurrrentTime();
            int newId = createNewId(DBUtil.COURSES_COLLECTION_NAME); // generate a new ID with auto-increasement
            boolean ret = add(DBUtil.getCollection(), new Document()
                    .append("id", newId)
                    .append("name", name)
                    .append("status", status)
                    .append("createdAt", timeStamp)
                    .append("updatedAt", timeStamp));
            if (ret) {
                return Response.status(Status.CREATED).location(URI.create("/courses/" + newId)).build();
            } else {
                return Response.status(Status.CONFLICT).build();
            }
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Find and update a course record with given id, name and status
     * 
     * @param id
     * @param jsonRequest
     * @return 
     */
    @PUT
    @Path("/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateCourse(@PathParam("id") int id, String jsonRequest) {
        JSONObject jsonObj = DataUtil.readJsonStr(jsonRequest);
        if (jsonObj == null) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        String name = (String) jsonObj.get("name");
        String status = (String) jsonObj.get("status");
        // validate input params
        if (name == null || "".equals(name.trim()) || status == null || !CourseStatus.isValid(status)) {
            return Response.status(Status.BAD_REQUEST).build();
        }
        // update database
        try {
            String timeStamp = DataUtil.getCurrrentTime();
            
            // Find the record and check the availability
            Document ret = find(DBUtil.getCollection(), eq("id", id));
            if (ret == null) {
                return Response.status(Status.NOT_FOUND).build();
            } else if (ret.get("deletedAt") != null){
                return Response.status(Status.GONE).build();
            }
            // Update the record
            ret = update(DBUtil.getCollection(), and(eq("id", id), exists("deletedAt", false)), combine(
                    set("name", name),
                    set("status", status),
                    set("updatedAt", timeStamp)));
            if (ret != null) {
                return Response.status(Status.ACCEPTED).build();
            } else {
                return Response.status(Status.GONE).build();
            }
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * DELETE the course by given id
     * 
     * @param id
     * @return 
     */
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteCourse(@PathParam("id") int id) {
        
        try {
            String timeStamp = DataUtil.getCurrrentTime();
            // Find the record and check the availability
            Document ret = find(DBUtil.getCollection(), eq("id", id));
            if (ret == null) {
                return Response.status(Status.NOT_FOUND).build();
            } else if (ret.get("deletedAt") != null){
                return Response.status(Status.GONE).build();
            }
            // mark the record as deleted
            ret = update(DBUtil.getCollection(), and(eq("id", id), exists("deletedAt", false)), combine(
                    set("updatedAt ", timeStamp),
                    set("deletedAt", timeStamp)));
            if (ret != null) {
                return Response.status(Status.NO_CONTENT).build();
            } else {
                return Response.status(Status.GONE).build();
            }
        } catch (Exception ex) {
            LOG.warn(ex.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}