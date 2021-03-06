package com.techsample.db.api;

import ch.qos.logback.classic.Logger;
import com.techsample.db.api.CoursesAPI.CourseStatus;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Meng Zhang
 */
public class CoursesAPITest extends JerseyTest {
    private final static Logger LOG = (Logger) LoggerFactory.getLogger(CoursesAPITest.class);
    private final static String BASE = "courses";

    @Override
    protected Application configure() {
//        ((Logger) LoggerFactory.getLogger("org.mongodb.driver")).setLevel(Level.INFO);
        ResourceConfig config = new ResourceConfig(CoursesAPI.class);
        config.register(MultiPartFeature.class);
        return config;
    }

    @Test
    public void testlistCourses() {
        LOG.info("testlistCourses");
        final String responseMsg = target().path(BASE).request().get(String.class);
        LOG.info(responseMsg);
//        assertEquals("{\"courses\":[{\"id\":1,\"name\":\"initla cource\"},{\"id\":2,\"name\":\"initla cource2\"}]}", responseMsg);
    }

    @Test
    public void testfindCourse() {
        LOG.info("testfindCourse");
        // normal record case
        String courseId = "2";
        String responseMsg = target().path(BASE).path(courseId).request().get(String.class);
        LOG.info(responseMsg);
        assertEquals("{\"id\": 2, \"name\": \"initla cource2\", \"status\": \"in_production\", \"updatedAt\": \"2001-02-04T04:05:06Z\", \"created_at\": \"2001-02-04T04:05:06Z\"}", responseMsg);
        
        // deleted record case
        try {
            courseId = "1";
            responseMsg = target().path(BASE).path(courseId).request().get(String.class);
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 410 Gone", ex.getMessage());
        }
        
        // no record case
        try {
            courseId = "999";
            responseMsg = target().path(BASE).path(courseId).request().get(String.class);
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 404 Not Found", ex.getMessage());
        }
        
    }
    
    @Test
    public void testCreateCourse() {
        LOG.info("testCreateCourse");
        String courseName = "course" + System.currentTimeMillis();
        // normal create case
        try {
            Response response = target().path(BASE).request().post(Entity.json("{\"name\":\"" + courseName + "\", \"status\":\"" + CourseStatus.scheduled.toString() + "\"}"));
            LOG.info(response.toString());
            LOG.info(response.getLocation().toString());
            LOG.info(target().path(response.getLocation().getPath()).request().get(String.class));
            assertEquals(201, response.getStatus());
//            assertEquals("http://localhost:9998/courses/3", response.getLocation().toString());
            
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
//            assertEquals("HTTP 404 Not Found", ex.getMessage());
        }
        
        // duplcate error
        try {
            Response response = target().path(BASE).request().post(Entity.json("{\"name\":\"" + courseName + "\", \"status\":\"" + CourseStatus.scheduled.toString() + "\"}"));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 400 Bad Request", ex.getMessage());
        }
        
        // missing value error
        try {
            Response response = target().path(BASE).request().post(Entity.json("{\"status\":\"" + CourseStatus.scheduled.toString() + "\"}"));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 400 Bad Request", ex.getMessage());
        }
        
        // blank value error
        try {
            Response response = target().path(BASE).request().post(Entity.json("{\"name\":\"\", \"status\":\"" + CourseStatus.scheduled.toString() + "\"}"));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 400 Bad Request", ex.getMessage());
        }
        
        // invalid value error
        try {
            Response response = target().path(BASE).request().post(Entity.json("{\"name\":\"" + courseName + "\", \"status\":\"scheduled2\"}"));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 400 Bad Request", ex.getMessage());
        }
    }
    
    @Test
    public void testUpdateCourse() {
        LOG.info("testUpdateCourse");
        String courseName = "course" + System.currentTimeMillis();
        // normal update case
        try {
            Response response = target().path(BASE).path("3").request().put(Entity.json("{\"name\":\"test789\", \"status\":\"" + CourseStatus.available.toString() + "\"}"));
            LOG.info(response.toString());
            assertEquals(202, response.getStatus());
            String responseMsg = target().path(BASE).path("3").request().get(String.class);
            LOG.info(responseMsg);
            
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
//            assertEquals("HTTP 404 Not Found", ex.getMessage());
        }
        
        // duplcated name error
        try {
            Response response = target().path(BASE).path("3").request().put(Entity.json("{\"name\":\"initla cource2\", \"status\":\"" + CourseStatus.available.toString() + "\"}"));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 400 Bad Request", ex.getMessage());
        }
        
        // missing value error
        try {
            Response response = target().path(BASE).path("3").request().put(Entity.json("{\"status\":\"" + CourseStatus.available.toString() + "\"}"));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 400 Bad Request", ex.getMessage());
        }
        
        // blank value error
        try {
            Response response = target().path(BASE).path("3").request().put(Entity.json("{\"name\":\"\", \"status\":\"" + CourseStatus.available.toString() + "\"}"));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 400 Bad Request", ex.getMessage());
        }
        
        // invalid value error
        try {
            Response response = target().path(BASE).path("3").request().put(Entity.json("{\"name\":\"test789\", \"status\":\"available2\"}"));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 400 Bad Request", ex.getMessage());
        }
        
        // update deleted record error
        try {
            Response response = target().path(BASE).path("1").request().put(Entity.form(new Form()
                    .param("name", "test123")
                    .param("status", "scheduled2")
                ));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 410 Gone", ex.getMessage());
        }
        
        // update non-existed record error
        try {
            Response response = target().path(BASE).path("99").request().put(Entity.form(new Form()
                    .param("name", "test123")
                    .param("status", "scheduled2")
                ));
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 404 Not Found", ex.getMessage());
        }
    }
    
    @Test
    public void testDeleteCourse() {
        LOG.info("testDeleteCourse");
        // normal delete case
        try {
            Response response = target().path(BASE).request().post(Entity.json("{\"name\":\"for delete\", \"status\":\"" + CourseStatus.scheduled.toString() + "\"}"));
            LOG.info(response.toString());
            LOG.info(response.getLocation().toString());
            Response response2 = target().path(response.getLocation().getPath()).request().delete();
            LOG.info(response2.toString());
            assertEquals(204, response2.getStatus());
            
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
//            assertEquals("HTTP 404 Not Found", ex.getMessage());
        }
        
        // Delete gone record error
        try {
            Response response = target().path(BASE).path("4").request().delete();
            
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 410 Gone", ex.getMessage());
        }
        
        
        try {
            Response response = target().path(BASE).path("99").request().delete();
            
        } catch (ClientErrorException ex) {
            LOG.info(ex.getMessage());
            assertEquals("HTTP 404 Not Found", ex.getMessage());
        }
    }
}
