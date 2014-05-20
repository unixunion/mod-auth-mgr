package org.vertx.mods.test.integration.javascript;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.testtools.TestVerticle;

import static org.vertx.testtools.VertxAssert.*;

/**
 * Created by keghol on 5/19/14.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAuth extends TestVerticle {

    EventBus eb;
    JsonObject config;
    String address;
    String userView;
    String designDoc;
    String persistorAddress;

    @Override
    public void start() {
        initialize();

        eb = vertx.eventBus();
        config = new JsonObject();

        this.address = container.config().getString("address", "vertx.basicauthmanager");
        this.userView = container.config().getString("user_view", "users");
        this.designDoc = container.config().getString("design_doc", "users");
        this.persistorAddress = container.config().getString("persistor_address", "vertx.couchpersistor");

        System.out.println("\n\n\nDeploy Mod Couchbase\n\n");


//        JsonObject couchConfig = container.config();

        JsonObject couchConfig = new JsonObject()
                .putBoolean("async_mode", true)
                .putString("address", "vertx.couchpersistor" )
                .putString("couchbase.nodelist", "localhost:8091")
                .putString("couchbase.bucket", "ivault")
                .putString("couchbase.bucket.password", "")
                .putNumber("couchbase.num.clients", 1);


        container.deployModule("com.deblox~mod-couchbase~0.0.1-SNAPSHOT", couchConfig, 1, new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> stringAsyncResult) {

                container.deployVerticle("org.vertx.mods.AuthManager", config, new AsyncResultHandler<String>() {

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
        });




    }



    @Test
    public void test_login() {
        container.logger().info("testing login");
        JsonObject request = new JsonObject()
                .putString("username", "user0")
                .putString("password", "somepassword");

        vertx.eventBus().send(address + ".login", request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> reply) {
                System.out.println(reply.body().toString());
                assertEquals("ok", reply.body().getString("status"));
                assertNotNull(reply.body().getString("sessionID"));
                testComplete();
            }
        });

    }

    @Test
    public void test_logout() {
        /*
        login, then logout with the session ID
         */
        JsonObject request = new JsonObject()
                .putString("username", "user0")
                .putString("password", "somepassword");

        vertx.eventBus().send(address + ".login", request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> reply) {
                System.out.println(reply.body().toString());
                assertEquals("ok", reply.body().getString("status"));
                assertNotNull(reply.body().getString("sessionID"));

                JsonObject logout_request = new JsonObject()
                        .putString("sessionID", reply.body().getString("sessionID"));

                vertx.eventBus().send(address + ".logout", logout_request, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(final Message<JsonObject> reply) {
                        System.out.println(reply.body().toString());
                        assertEquals("ok", reply.body().getString("status"));
                        testComplete();
                    }
                });


            }
        });
    }


    @Test
    public void test_login_error_nousername() {
        /*
        login with bad data
         */
        JsonObject login_request = new JsonObject()
                .putString("notusername", "dsdsa");

        vertx.eventBus().send(address + ".login", login_request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> reply) {
                System.out.println(reply.body().toString());
                assertEquals("error", reply.body().getString("status"));
                testComplete();
            }
        });
    }

    @Test
    public void test_login_error_nopassword() {
        /*
        login with bad data
         */
        JsonObject login_request = new JsonObject()
                .putString("username", "dsdsa");

        vertx.eventBus().send(address + ".login", login_request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> reply) {
                System.out.println(reply.body().toString());
                assertEquals("error", reply.body().getString("status"));
                testComplete();
            }
        });
    }



    @Test
    public void test_logout_not_logged_in() {
        /*
        login, then logout with the session ID
         */
        JsonObject logout_request = new JsonObject()
                .putString("sessionID", "08922120-7acf-4313-b5bd-e1444720eb3d");

        vertx.eventBus().send(address + ".logout", logout_request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> reply) {
                System.out.println(reply.body().toString());
                assertEquals("error", reply.body().getString("status"));
                testComplete();
            }
        });
    }


    @Test
    public void test_failed_login() {
        JsonObject request = new JsonObject()
                .putString("username", "user0")
                .putString("password", "dsadas");

        vertx.eventBus().send(address + ".login", request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> reply) {
                assertEquals("denied", reply.body().getString("status"));
                testComplete();
            }
        });

    }


    @Test
    public void test_authorize() {
        /*
        login, then authorize with the session ID
         */
        JsonObject request = new JsonObject()
                .putString("username", "user0")
                .putString("password", "somepassword");

        vertx.eventBus().send(address + ".login", request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> reply) {
                System.out.println(reply.body().toString());
                assertEquals("ok", reply.body().getString("status"));
                assertNotNull(reply.body().getString("sessionID"));

                JsonObject authorize_request = new JsonObject()
                        .putString("sessionID", reply.body().getString("sessionID"));

                vertx.eventBus().send(address + ".authorise", authorize_request, new Handler<Message<JsonObject>>() {
                    @Override
                    public void handle(final Message<JsonObject> reply) {
                        System.out.println(reply.body().toString());
                        assertEquals("ok", reply.body().getString("status"));
                        testComplete();
                    }
                });


            }
        });
    }



}
