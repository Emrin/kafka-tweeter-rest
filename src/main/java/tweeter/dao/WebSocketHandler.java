package tweeter.dao;

import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tweeter.resources.Tweet;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;


@WebSocket
public class WebSocketHandler {
    private Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private Gson gson = new Gson();
    private String location, tag, mention; // filters
    private final KafkaConsumer<String, Tweet> consumerWS;
    private static List<Session> users = new ArrayList<>();

    public WebSocketHandler(KafkaConsumer<String, Tweet> consumerWS) {
        this.consumerWS = consumerWS;
    }

    // Getters and setters
    public String getLocation() {
        return location;
    }
    public void setLocation(String location) {
        this.location = location;
    }

    public String getTag() {
        return tag;
    }
    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getMention() {
        return mention;
    }
    public void setMention(String mention) {
        this.mention = mention;
    }

    public void poll() {
        ConsumerRecords<String, Tweet> tweets = this.consumerWS.poll(Duration.ofMillis(500));
        tweets.forEach(t -> this.broadcast(gson.toJson(t.value())));
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
        System.out.println("broadcast message: "+message); // also send response 200
        users.stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
