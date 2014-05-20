package com.deblox.authservice;

import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.AsyncResultHandler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Server extends Verticle {

    JsonObject couchConfig;
    JsonObject authConfig;

    public void start() {

        couchConfig = loadConfig("/couch-conf.json");
        authConfig = loadConfig("/conf.json");

//        container.deployVerticle("AuthManager.java", 1);
//        container.deployVerticle("HttpVerticle.java", 16);

        System.out.println("Deploying mod-couchbase");
        container.deployModule("com.deblox~mod-couchbase~0.0.1", couchConfig, 1, new AsyncResultHandler<String>() {
            @Override
            public void handle(AsyncResult<String> stringAsyncResult) {

                System.out.println("Deploying mod-auth-mgr-couch");
                container.deployModule("com.deblox~mod-auth-mgr-couch~0.0.1", authConfig, 1, new AsyncResultHandler<String>() {

                    @Override
                    public void handle(AsyncResult<String> asyncResult) {

                        // Deployment is asynchronous and this this handler will be called when it's complete (or failed)
                        if (asyncResult.failed()) {
                            container.logger().error(asyncResult.cause());
                        }

                        if (asyncResult.succeeded()) {
                            System.out.println("Deploying HttpVerticle");
                            container.deployVerticle("HttpVerticle.java", 2);
                        }


                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();


                        }


                    }
                }); // end auth-mgr
            } // end handle

        }); // end couch



    } // end start


    private JsonObject loadConfig(String file) {
        System.out.println(System.getProperty("java.class.path"));
        try (InputStream stream = this.getClass().getResourceAsStream(file)) {
            StringBuilder sb = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));

            String line = reader.readLine();
            while (line != null) {
                sb.append(line).append('\n');
                line = reader.readLine();
            }

            return new JsonObject(sb.toString());

        } catch (IOException e) {
            e.printStackTrace();
            return new JsonObject();
        }

    }

}
