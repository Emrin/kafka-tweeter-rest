package it.polimi.deib.middleware.rest.tweeter;

import it.polimi.deib.middleware.rest.commons2.AbstractService;
import it.polimi.deib.middleware.rest.commons2.resources.Resource;
import it.polimi.deib.middleware.rest.commons2.resources.serizalization.ResourceDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;

import static spark.Spark.*;

public class Websocket extends AbstractService {

    private String location, tag, mention;

    // constructor
    public void Construct(String location, String tag, String mention){
        this.location = location;
        this.tag = tag;
        this.mention = mention;
    }

    private static KafkaConsumer<String, Resource> consumer;

    public void run() {

        logger = LoggerFactory.getLogger(Websocket.class);

        Properties props = new Properties();

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer" + UUID.randomUUID().toString());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResourceDeserializer.class);

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("tweets1"));

        port(4242);

        WebSocketHandler wsh = new WebSocketHandler(consumer);

        webSocket("/ws", wsh);
        init();

        Thread t = new Thread(() -> {
            consumer.poll(Duration.ofMillis(100));
            Set<TopicPartition> assignment = consumer.assignment();
            consumer.seekToBeginning(assignment);

            while (true) {
                logger.info("Polling...");
                wsh.poll();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();

    }

    @WebSocket
    public static class WebSocketHandler {

        private final KafkaConsumer<String, Resource> consumer;
        List<Session> users = new ArrayList<>();

        public WebSocketHandler(KafkaConsumer<String, Resource> consumer) {
            this.consumer = consumer;
        }

        public void poll() {
            ConsumerRecords<String, Resource> orders = this.consumer.poll(Duration.ofMillis(500));
            orders.forEach(pr -> this.broadcast(gson.toJson(pr.value())));
        }

        @OnWebSocketConnect
        public void onConnect(Session user) throws Exception {
            users.add(user);
            logger.info("A user joined the chat");
        }

        @OnWebSocketClose
        public void onClose(Session user, int statusCode, String reason) {
            users.remove(user);
            logger.info("A user left the chat");
        }

        @OnWebSocketMessage
        public void onMessage(Session user, String message) {
            logger.info("Session user: "+user.toString());
            logger.info("Websocket message: "+message);
        }

        private void broadcast(String message) {
            logger.info("Broadcast called");
            users.stream().filter(Session::isOpen).forEach(session -> {
                try {
                    session.getRemote().sendString(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }
}