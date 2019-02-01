package tweeter.dao;

import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@WebSocket
public class WebSocketHandler {
    private Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private HashMap<String, Tweet> tweets = new HashMap<>();
    private List<String> tweet_ids = new ArrayList<>();
    private final KafkaConsumer<String, Tweet> consumerWS;
    // for each user: [location, tag, mention] filters.
    private static Map<Session, List<String>> userFilterMap = new ConcurrentHashMap<>(); // map users to their filters
    // todo add user progress in the hashmap to see where user left off. nah.
    public WebSocketHandler(KafkaConsumer<String, Tweet> consumerWS) {
        this.consumerWS = consumerWS;

        Thread t = new Thread(() -> {
            consumerWS.poll(Duration.ofMillis(100));
            Set<TopicPartition> assignment = consumerWS.assignment();
            consumerWS.seekToBeginning(assignment);
            while (true) {
                logger.info("WS polling...");
                poll();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    public void poll() {
        ConsumerRecords<String, Tweet> tweets = this.consumerWS.poll(Duration.ofMillis(500));
        tweets.forEach(t -> this.broadcast(t.value())); // value is tweet
    }

    @OnWebSocketConnect
    public void onConnect(Session user) throws Exception {
        List<String> filter = Arrays.asList(null, null, null);
        userFilterMap.put(user, filter);
        logger.info("A user joined.");
        send(user);
    }

    @OnWebSocketClose
    public void onClose(Session user, int statusCode, String reason) {
        userFilterMap.remove(user);
        logger.info("A user left.");
    }

    // Upon receiving a message by the user, change his personal filter.
    @OnWebSocketMessage // works
    public void onMessage(Session user, String message) {
        logger.info("Session user: "+user.toString());
        logger.info("Websocket message: "+message);
//        int indexOfUser = users.indexOf(user);
        Map<String, String> params = getQueryMap(message);
        String location = (String)params.get("location");
        String tag = (String)params.get("tag");
        String mention = (String)params.get("mention");
        logger.info("queryParamsMap.keys() = "+params.keySet() + "\nqueryParamsMap.values() = "+params.values());
        logger.info("loc : "+location+" , tag : "+tag+" , mention : "+mention);
        List<String> newFilter = Arrays.asList(location, tag, mention);
        userFilterMap.replace(user, newFilter);
        send(user);
    }

    // Broadcast new tweets, and send tweets for new connections.
    private void broadcast(Tweet tweet) { // works
        logger.info("Broadcast called");
        logger.info("broadcast message: "+tweet.toString()); // also send response 200
        List<Session> usersList = new ArrayList<Session>(userFilterMap.keySet());
        usersList.stream().filter(Session::isOpen).forEach(userSession -> {
            try {
                // for every connected user,
                // if this tweet is what the user is looking for
                logger.info("Attempting to broadcast.");
                if (tweet.filterLoc(userFilterMap.get(userSession).get(0)) ||
                    tweet.filterMention(userFilterMap.get(userSession).get(1)) ||
                    tweet.filterTag(userFilterMap.get(userSession).get(2))){
                    userSession.getRemote().sendString(tweet.toString()); // send.
                    logger.info("Broadcast successful.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    // Send all tweets to this user, used for when he connects.
    private void send(Session user){ // todo fix
        List<Tweet> serving = matchmaker(tweets, tweet_ids, userFilterMap.get(user).get(0),
                userFilterMap.get(user).get(1), userFilterMap.get(user).get(2)); // userFilterMap's value is the filter array.
        for (Tweet tweet : serving){
            try {
                user.getRemote().sendString(tweet.toString());
            } catch (Exception e){
                logger.info(e.toString());
            }
        }
    }

    // Iterate over all tweets and if one of them has a matching arg, add it to the result.
    private List<Tweet> matchmaker(HashMap<String, Tweet> tweets, List<String> tweet_ids, String location,
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

    public void addTweet(Tweet tweet, String tweet_id) {
        this.tweets.put(tweet.getId(), tweet);
        this.tweet_ids.add(tweet_id);
        // todo broadcast new tweets
        broadcast(tweet);
    }

    private static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params)
        {  String [] p=param.split("=");
            String name = p[0];
            if(p.length>1)  {String value = p[1];
                map.put(name, value);
            }
        }
        return map;
    }

}
