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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@WebSocket
public class WebSocketHandler {
    private Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private Gson gson = new Gson();
    private String location, tag, mention; // filters
    private final KafkaConsumer<String, Tweet> consumerWS;
    private static List<Session> users = new ArrayList<>();

    // this map is shared between sessions and threads, so it needs to be thread-safe (http://stackoverflow.com/a/2688817)
    static Map<Session, String> userUsernameMap = new ConcurrentHashMap<>();
    static int nextUserNumber = 1; //Assign to username for next connecting user
    private String sender, msg;

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
        // broadcast
    }

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        users.add(user);
        logger.info("A user joined the chat");

        String username = "User" + nextUserNumber++;
        userUsernameMap.put(user, username);
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        users.remove(user);
        logger.info("A user left the chat");

        String username = userUsernameMap.get(user);
        userUsernameMap.remove(user);
    }

    @OnWebSocketMessage
    public void onMessage(Session user, String message) {
        logger.info("Session user: "+user.toString());
        logger.info("Websocket message: "+message);
        broadcastMessage(message);
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

    // Iterate over all tweets and if one of them has a matching arg, add it to the result.
    public List<Tweet> matchmaker(HashMap<String, Tweet> tweets, List<String> tweet_ids, String location,
                                          String tag, String mention) {
        List<Tweet> result = new ArrayList<Tweet>();
        if (!tweets.isEmpty()) {
            for (int i = 0; i < tweets.size(); i++) {
                if (tweets.get(tweet_ids.get(i)).filterLoc(location) || tweets.get(tweet_ids.get(i)).filterTag(tag) ||
                        tweets.get(tweet_ids.get(i)).filterMention(mention)) {
                    result.add(tweets.get(tweet_ids.get(i))); // spaghetti = very much
                    System.out.println("Found filter matching to tweet with id = " + i);
                }
            }
        }
        System.out.println("Total size of matching tweets = "+result.size());
        return result;
    }

    //Sends a message from WS to open sessions
    public void broadcastMessage(String message) {
        logger.info("Called broadcastMessage.");
        userUsernameMap.keySet().stream().filter(Session::isOpen).forEach(session -> {
            try {
                session.getRemote().sendString(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }


}
