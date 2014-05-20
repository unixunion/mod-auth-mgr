package com.deblox.authservice;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.EventBus;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class ProcessorVerticle extends Verticle {
    private EventBus eventBus;
    private JsonObject reply = new JsonObject();
    private byte[] buffer = new byte[10];

    public void start() {
        eventBus = vertx.eventBus();

        /*
        login-handler

        request users password hash from the couch database,
        compare the hashes
        returh a true/false

         */
        eventBus.registerHandler("login-handler", new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                try {
                    message.reply(new JsonObject().putBoolean("status", true));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    message.reply(new JsonObject().putBoolean("status", false));
                }
            }
        });

        eventBus.registerHandler("handler2", new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                try {
                    //Thread.sleep(1000);
                } catch (Exception ex) {
                }
            }
        });
        eventBus.registerHandler("handler3", new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                try {
                    //Thread.sleep(1000);
                } catch (Exception ex) {
                }
                message.reply(reply);
            }
        });
        eventBus.registerHandler("handler5", new Handler<Message<byte[]>>() {
            public void handle(Message<byte[]> message) {
                try {
                    //Thread.sleep(1000);
                } catch (Exception ex) {
                }
                message.reply(buffer);
            }
        });
        eventBus.registerHandler("handler4", new Handler<Message<JsonObject>>() {
            public void handle(Message<JsonObject> message) {
                try {
                    //Thread.sleep(1000);
                } catch (Exception ex) {
                }

                String address = message.body().getString("address");
                Long sequenceId = message.body().getLong("sequenceId");
                reply.putNumber("sequenceId", sequenceId);
                eventBus.send(address, reply);
            }
        });

    }
}
