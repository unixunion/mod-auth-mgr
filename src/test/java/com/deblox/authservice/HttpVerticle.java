package com.deblox.authservice;

import org.vertx.java.core.Handler;
import org.vertx.java.core.buffer.Buffer;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.HttpServerResponse;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HttpVerticle extends Verticle {
    private static final int SERVER_PORT = 8080;
    protected EventBus eventBus;
    private RouteMatcher routeMatcher;
    protected JsonObject message = new JsonObject();
    protected byte[] buffer = new byte[10];
    private final String uuid = UUID.randomUUID().toString();
    protected Map<Long, HttpServerResponse> requests = new ConcurrentHashMap<Long, HttpServerResponse>();
    protected Long sequenceId = new Long(0);
    JsonObject config;
    String modAuthAddress;

    public void start() {
        eventBus = vertx.eventBus();
        routeMatcher = new RouteMatcher();


        config = container.config();
        modAuthAddress =  config.getString("auth_address", "vertx.basicauthmanager");

        System.out.println("Starting HttpServer");

        /*
        accept a json document with username and passwordHash,
        send a message to the login-handler
        issue a token if auth-success
         */
        routeMatcher.post("/login", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {

//                System.out.println("Got request: " + req.uri());
//                System.out.println("Headers: ");
//
//                for (Map.Entry<String, String> entry : req.headers()) {
//                    System.out.println(entry.getKey() + ":" + entry.getValue());
//                }
//
//                System.out.print("sending to " + modAuthAddress + ".login");

                req.bodyHandler(new Handler<Buffer>() {
                    @Override
                    public void handle(Buffer event) {
//                        System.out.println(event);
                        JsonObject document = new JsonObject(event.toString());
                        eventBus.send(modAuthAddress + ".login", document, new Handler<Message<JsonObject>>() {

                            public void handle(Message<JsonObject> message) {
//                                System.out.println("response from .login: " + message.body());
                                if (message.body().getString("status").equals("ok")) {
                                    req.response().setStatusCode(200);
                                    req.response().end("SUCCESS");
                                } else {
                                    req.response().setStatusCode(503);
                                    req.response().end("FAILURE");
                                }

                            }
                        });
                    }
                });
            }
        });

        routeMatcher.get("/handler1", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
               req.response().end();
            }
        });

        routeMatcher.get("/handler2", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                eventBus.send("handler2", message);
                req.response().end();
            }
        });

        routeMatcher.get("/handler3", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                eventBus.send("handler3", message, new Handler<Message<JsonObject>>() {
                    public void handle(Message<JsonObject> message) {
                        req.response().end();
                    }
                });
            }
        });

        routeMatcher.get("/handler4", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                requests.put(sequenceId, req.response());
                message.putString("address", uuid);
                message.putNumber("sequenceId", sequenceId);
                eventBus.send("handler4", message);
                sequenceId++;
            }
        });

        routeMatcher.get("/handler5", new Handler<HttpServerRequest>() {
            public void handle(final HttpServerRequest req) {
                eventBus.send("handler5", buffer, new Handler<Message<byte[]>>() {
                    public void handle(Message<byte[]> message) {
                        req.response().end();
                    }
                });
            }
        });
        routeMatcher.noMatch(new Handler<HttpServerRequest>() {
            public void handle(HttpServerRequest req) {
                req.response().end("Nothing matched");
            }
        });

        eventBus.registerHandler(uuid, new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                try {
                    Long sequenceId = message.body().getLong("sequenceId");
                    HttpServerResponse response = requests.get(sequenceId);
                    response.end();
                } catch (Exception ex) {
                    System.out.println(sequenceId);
                }
            }
        });

        eventBus.registerHandler("http-verticle", new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                JsonObject reply = new JsonObject();
                message.reply(reply);
            }
        });

        System.out.println("Starting to Listen");
        HttpServer server = vertx.createHttpServer().requestHandler(routeMatcher).setAcceptBacklog(50).listen(SERVER_PORT);


    }
}
