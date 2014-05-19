package org.vertx.mods.test.integration.javascript;

import org.junit.Test;
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

        System.out.println("\n\n\nDeploy Worker Verticle Couchbase Sync\n\n");


        JsonObject couchConfig = new JsonObject()
                .putBoolean("async_mode", false)
                .putString("address", "vertx.couchpersistor" )
                .putString("couchbase.nodelust", "localhost:8091")
                .putString("couchbase.bucket", "ivault")
                .putString("couchbase.bucket.password", "")
                .putNumber("couchbase.num.clients", 1);


        container.deployModule("com.scalabl3~vertxmods.couchbase~1.0.0-final", couchConfig, 1, new AsyncResultHandler<String>() {
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
    public void test1() {
        JsonObject request = new JsonObject()
                .putString("username", "user0")
                .putString("password", "somepassword");

        vertx.eventBus().send(address + ".login", request, new Handler<Message<JsonObject>>() {
            @Override
            public void handle(final Message<JsonObject> reply) {
                System.out.println(reply.body().toString());
                testComplete();
            }
        });

    }


}
