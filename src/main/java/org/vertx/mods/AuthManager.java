/*
 * Copyright 2011-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.vertx.mods;

import org.vertx.java.busmods.BusModBase;
import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.Message;
import org.vertx.java.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Basic Authentication Manager Bus Module<p>
 * Please see the busmods manual for a full description<p>
 *
 * @author <a href="http://tfox.org">Tim Fox</a>
 */
public class AuthManager extends BusModBase {

  private Handler<Message<JsonObject>> loginHandler;
  private Handler<Message<JsonObject>> logoutHandler;
  private Handler<Message<JsonObject>> authoriseHandler;

  protected final Map<String, String> sessions = new HashMap<>();
  protected final Map<String, LoginInfo> logins = new HashMap<>();

  private static final long DEFAULT_SESSION_TIMEOUT = 30 * 60 * 1000;

  private String address;
  private String userView;
  private String designDoc;
  private String persistorAddress;
  private long sessionTimeout;

  private static final class LoginInfo {
    final long timerID;
    final String sessionID;

    private LoginInfo(long timerID, String sessionID) {
      this.timerID = timerID;
      this.sessionID = sessionID;
    }
  }

  /**
   * Start the busmod
   */
  public void start() {
    super.start();

    this.address = getOptionalStringConfig("address", "vertx.basicauthmanager");
    this.userView = getOptionalStringConfig("user_view", "users");
    this.designDoc = getOptionalStringConfig("design_doc", "users");
    this.persistorAddress = getOptionalStringConfig("persistor_address", "vertx.couchpersistor");
    Number timeout = config.getNumber("session_timeout");

    if (timeout != null) {
      if (timeout instanceof Long) {
        this.sessionTimeout = (Long)timeout;
      } else if (timeout instanceof Integer) {
        this.sessionTimeout = (Integer)timeout;
      }
    } else {
      this.sessionTimeout = DEFAULT_SESSION_TIMEOUT;
    }

    loginHandler = new Handler<Message<JsonObject>>() {
      public void handle(Message<JsonObject> message) {
        doLogin(message);
      }
    };
    eb.registerHandler(address + ".login", loginHandler);
    container.logger().info("loginHandler listening on: " + address + ".login");

    logoutHandler = new Handler<Message<JsonObject>>() {
      public void handle(Message<JsonObject> message) {
        doLogout(message);
      }
    };
    eb.registerHandler(address + ".logout", logoutHandler);
    container.logger().info("logoutHandler listening on: " + address + ".logout");

    authoriseHandler = new Handler<Message<JsonObject>>() {
      public void handle(Message<JsonObject> message) {
        doAuthorise(message);
      }
    };
    eb.registerHandler(address + ".authorise", authoriseHandler);
    container.logger().info("authorizeHandler listening on: " + address + ".authorise");
  }

  private void doLogin(final Message<JsonObject> message) {

    container.logger().debug("got message: " + message.body().toString());

    final String username = getMandatoryString("username", message);
    if (username == null) {
      return;
    }
    String password = getMandatoryString("password", message);
    if (password == null) {
      return;
    }

    JsonObject request = new JsonObject().putString("op", "QUERY")
          .putString("design_doc", designDoc)
          .putString("view_name", userView)
          .putString("key", "[\"" + username + "\",\"" + password + "\"]")
          .putBoolean("include_docs", true)
          .putBoolean("ack", true);


    eb.send(persistorAddress, request, new Handler<Message<JsonObject>>() {
      public void handle(Message<JsonObject> reply) {
        container.logger().debug(reply.body().toString());
        JsonObject response = reply.body().getObject("response").getObject("response");
        container.logger().debug("response from persistor: " + response);
        container.logger().debug("response result: " + response.getArray("result").size());

        if (response.getBoolean("success").equals(true)) {
          // if nothing in the results from couch
          if (response.getArray("result").size() != 0) {

            // Check if already logged in, if so logout of the old session
            LoginInfo info = logins.get(username);
            if (info != null) {
              logout(info.sessionID);
            }

            // Found
            final String sessionID = UUID.randomUUID().toString();
            long timerID = vertx.setTimer(sessionTimeout, new Handler<Long>() {
              public void handle(Long timerID) {
                sessions.remove(sessionID);
                logins.remove(username);
              }
            });
            sessions.put(sessionID, username);
            logins.put(username, new LoginInfo(timerID, sessionID));
            JsonObject jsonReply = new JsonObject().putString("sessionID", sessionID);
            sendOK(message, jsonReply);
          } else {
            // Not found
            sendStatus("denied", message);
          }
        } else {
          logger.error("Failed to execute login query: " + reply.body().getString("message"));
          sendError(message, "Failed to excecute login");
        }
      }
    });
  }

  protected void doLogout(final Message<JsonObject> message) {
    final String sessionID = getMandatoryString("sessionID", message);
    if (sessionID != null) {
      if (logout(sessionID)) {
        sendOK(message);
      } else {
        super.sendError(message, "Not logged in");
      }
    }
  }

  protected boolean logout(String sessionID) {
    String username = sessions.remove(sessionID);
    if (username != null) {
      LoginInfo info = logins.remove(username);
      vertx.cancelTimer(info.timerID);
      return true;
    } else {
      return false;
    }
  }

  protected void doAuthorise(Message<JsonObject> message) {
    String sessionID = getMandatoryString("sessionID", message);
    if (sessionID == null) {
      return;
    }
    String username = sessions.get(sessionID);

    // In this basic auth manager we don't do any resource specific authorisation
    // The user is always authorised if they are logged in

    if (username != null) {
      JsonObject reply = new JsonObject().putString("username", username);
      sendOK(message, reply);
    } else {
      sendStatus("denied", message);
    }
  }


}
