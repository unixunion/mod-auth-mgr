package org.vertx.mods.test.integration.javascript;

import org.junit.Test;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.vertx.testtools.VertxAssert.*;


public class GenerateTestAccounts extends TestVerticle{

    JsonObject config;

    // timers
    long startTime;
    long endTime;
    long timeEnded;
    Integer count = 1;
    Integer count_max = 1;

    @Override
    public void start() {
        initialize();

        EventBus eb = vertx.eventBus();
        config = new JsonObject();

        config.putString("address", "vertx.couchbase.async");
        config.putString("couchbase.nodelist", "localhost:8091");
        config.putString("couchbase.bucket", "default");
        config.putString("couchbase.bucket.password", "");
        config.putNumber("couchbase.num.clients", 1);
        config.putBoolean("async_mode", true);

        System.out.println("\n\n\nDeploy Worker Verticle Couchbase Async\n\n");

        container.deployModule("com.deblox~mod-couchbase~0.0.2-SNAPSHOT", config, 1, new AsyncResultHandler<String>() {

            @Override
            public void handle(AsyncResult<String> asyncResult) {

                // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
                if (asyncResult.failed()) {
                    container.logger().error(asyncResult.cause());
                }

                assertTrue(asyncResult.succeeded());
                assertNotNull("deploymentID should not be null", asyncResult.result());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // If deployed correctly then start the tests!
                startTests();
            }
        });


    }

    // used to count async results and finalize tests
    public void count() {
        count=count+1;
        if (count > count_max-1) {
            endTime = System.currentTimeMillis();
            timeEnded =  ((endTime-startTime) /1000);
            System.out.println("rate achieved: " + (count_max/timeEnded) + " msgs/ps");
            count_max=1;
            count=0;
            testComplete();
        }
    }

    // Create a new user object via User class
    public void add(String username) {

//        String hashed = BCrypt.hashpw("somepassword", BCrypt.gensalt(4));
        String hashed = null;
        try {
            hashed = getHash("somepassword");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        User user = new User(username, hashed);
        String user_string = Util.encode(user);

        JsonObject request = new JsonObject().putString("op", "ADD")
                .putString("key", user.getUsername())
                .putString("value", user_string)
                .putNumber("expiry", 86400)
                .putBoolean("ack", true);

        container.logger().debug("sending message to address: " + config.getString("address"));

        vertx.eventBus().send(config.getString("address"), request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(Message<JsonObject> reply) {
                try {
                    container.logger().debug("Add response: " + reply.body().toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("shit happens");
                }
            }
        });
    }

    @Test
    public void add_keys() {
        count_max = 100000;
        startTime = System.currentTimeMillis();

        for(int i=0; i < count_max; i++) {
            add("user" + i);
            count();
        }
    }

    @Test
    public void get_keys() {

        add("user" + 1001);

        JsonObject request = new JsonObject().putString("op", "GET")
                .putString("key", "user1001")
                .putBoolean("include_docs", true)
                .putBoolean("ack", true);

        vertx.eventBus().send(config.getString("address"), request, new Handler<Message<JsonObject>>() {

            @Override
            public void handle(final Message<JsonObject> reply) {
                System.out.println("Try to deserialize reply: " + reply.body().toString());

                try {
                    String user = reply.body()
                            .getObject("response")
                            .getObject("data")
                            .getString("value");
                    User u = (User)Util.decode(user, User.class );

                    System.out.println("UserObject password: " + u.getPassword());

                    if (getHash("somepassword").equals(u.getPassword())){
                        testComplete();
                    } else {
                        System.out.println("Error, password missmatch, check your data: " + u.getPassword() + " : " + u.toString());
                        System.out.println("reply was: " + reply.body());
                        fail();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    fail();

                }
            }
        });
    }

    public String getHash(String password) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.reset();
        byte[] input = digest.digest(password.getBytes("UTF-8"));

        //convert the byte to hex format method 2
        StringBuffer hexString = new StringBuffer();
        for (int i=0;i<input.length;i++) {
            hexString.append(Integer.toHexString(0xFF & input[i]));
        }

        return hexString.toString();
    }


}
