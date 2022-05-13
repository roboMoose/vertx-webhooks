package com.example.vertx_webhooks;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;


@ExtendWith(VertxExtension.class)
public class TestMainVerticle {


  private static final Logger LOG = LoggerFactory.getLogger(TestMainVerticle.class);
  public static final int EXPECTED_MESSAGES = 5;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  @Test
  void can_connect_to_websocket_server(Vertx vertx, VertxTestContext context) throws Throwable {
    HttpClient client = vertx.createHttpClient();
    client.webSocket(8900, "localhost", WebSockerHandler.PATH)
      .onFailure(context::failNow)
      .onComplete(context.succeeding(ws -> {
          ws.handler(data -> {
            String receivedData = data.toString();
            LOG.debug("Received message: {}", receivedData);
            assertEquals("Connected!", receivedData);
            client.close();
            context.completeNow();
          });
        })
      );
  }

  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  @Test
  void receive_multiple_messages(Vertx vertx, VertxTestContext context) throws Throwable {
    HttpClient client = vertx.createHttpClient();

    final AtomicInteger counter = new AtomicInteger(0);
    client.webSocket(8900, "localhost", WebSockerHandler.PATH)
      .onFailure(context::failNow)
      .onComplete(context.succeeding(ws -> {
          ws.handler(data -> {
            String receivedData = data.toString();
            LOG.debug("Received message: {}", receivedData);
            int currentValue = counter.getAndIncrement();
            if(currentValue >= EXPECTED_MESSAGES){
              client.close();
              context.completeNow();
            }else{
             LOG.debug("Not enough messages yet... {{}/{}}", currentValue, EXPECTED_MESSAGES);
            }
          });
        })
      );
  }
}
