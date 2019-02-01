package tweeter;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Session;
import tweeter.api.commons.AbstractService;
import tweeter.api.commons.Resp;
import tweeter.dao.*;
import tweeter.resources.Tweet;
import tweeter.resources.User;
import tweeter.resources.serizalization.TweetDeserializer;

import java.time.Duration;
import java.util.*;

import static spark.Spark.*;


public class TweeterApp extends AbstractService {

    static Boolean called = false;

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(TweeterApp.class);
        String topic = "tweeter3";
        String topicUsers = "users2";
        int portNum = 4242; // for web requests.
//        HashMap<String, Tweet> tweets = new HashMap<>();
//        List<String> tweet_ids = new ArrayList<>();
        List<Session> users = new ArrayList<>();

        port(portNum);

        // Producer
        Producer producer = new Producer(topic);
        ProducerUsers producerUsers = new ProducerUsers(topicUsers);

        // Consumer
        Consumer consumer = new Consumer(topic);
        ConsumerUsers consumerUsers = new ConsumerUsers(topicUsers);

        // ConsumerWS
        Properties props = new Properties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer" + UUID.randomUUID().toString());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, TweetDeserializer.class);
        KafkaConsumer<String, Tweet> consumerWS = new KafkaConsumer<>(props);
        consumerWS.subscribe(Collections.singletonList(topic));
        WebSocketHandler wsh = new WebSocketHandler(consumerWS);
        webSocket("/ws", wsh);
        init();


        // Kstream push to WS broadcast
        Properties propsStream = new Properties();
        propsStream.put(StreamsConfig.APPLICATION_ID_CONFIG, "tweets-stream");
        propsStream.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propsStream.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        propsStream.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, TweetDeserializer.class);
        final StreamsBuilder builder = new StreamsBuilder();
        KStream<String, Tweet> source = builder.stream(topic);
        source.foreach((String a, Tweet t) -> {
            wsh.place(t, t.getId());
        });

        try{ // this try catch fixes a bug that occurs on first calls.
            Thread.sleep(500);
            consumerUsers.poll("x");
            consumer.pollInitial();
        }catch (Exception e) {
            logger.info("Exception "+e);
        }

        // Initialize the tweets hashmap with already-existing tweets from topic.
        HashMap<String, Tweet> tweets = consumer.getTweetsMapMap();
        List<String> tweet_ids = consumer.getTweetIds();

        path("/users", () -> {
            before("/*", (q, a) -> logger.info("Api call to /users."));

            post("/:username", (request, response) -> {
                // Check if user exists in users.
                String username = request.params(":username");
                if (consumerUsers.poll(username)) {
                    // If user already exists.
                    return gson.toJson(new Resp(CLIENT_ERROR + 9, "User already exists."));
                }else { // Push new user.
                    User user = producerUsers.register(gson.fromJson(request.body(), User.class), username);
                    return gson.toJson(new Resp(SUCCESS, "New user added: " + user.toString()));
                }
            });
        });

        path("/tweets", () -> {
            before("/*", (q, a) -> logger.info("Received api call to /tweets"));

            // User posts a tweet.
            post("", (request, response) -> { // User posted a new tweet
                logger.info("Post requesst.");
                response.type("application/json");
                response.status(SUCCESS);
                String body = request.body();
                try {
                    if (body != null && !body.isEmpty()) { // maybe check form with regex
                        Tweet thisTweet = gson.fromJson(request.body(), Tweet.class);
                        // Check if user is registered.
                        if(consumerUsers.poll(thisTweet.getAuthor())) {
                            producer.place(thisTweet);
                            tweets.put(thisTweet.getId(), thisTweet);
                            tweet_ids.add(thisTweet.getId());
                            return gson.toJson(new Resp(SUCCESS, "Tweet Created: [" + thisTweet.toString() + "]"));
                        }
                        else{
                            return gson.toJson(new Resp(CLIENT_ERROR + 9, "Author doesn't exist."));
                        }
                    } else
                        return gson.toJson(new Resp(400, "Bad Request"));
                } catch (Exception e) {
                    e.printStackTrace();
                    return gson.toJson(new Resp(400, "Bad Request"));
                }
            });

            // User GET request.
            get("", (request, response) -> {
                List<Tweet> mapTweets = new ArrayList<>(tweets.values());
                logger.info("Get request with no filter.");
                return gson.toJson(new Resp(SUCCESS, gson.toJson(tweets.values())));
            });

            // GET request with filter.
            get("/:filter", (request, response) -> { // /location=Awesomeville&tag=Art&mention=Trees
                // If filter is empty, return everything.
                if (request.params(":filter").isEmpty()) {
                    return gson.toJson(new Resp(SUCCESS, gson.toJson(tweets.values())));
                }

                logger.info("Get request with filter.");
                Map<String, String> params = getQueryMap(request.params(":filter"));
                String location = (String)params.get("location");
                String tag = (String)params.get("tag");
                String mention = (String)params.get("mention");
                logger.info("\nqueryParamsMap.keys() = "+params.keySet() + "\nqueryParamsMap.values() = "+params.values());
                logger.info("loc : "+location+" , tag : "+tag+" , mention : "+mention);

                // Get list of tweets that have matching args.
                List<Tweet> matches = matchmaker(tweets, tweet_ids, location, tag, mention);
                logger.info("Length of matches = "+matches.size());
                for(Tweet tweet : matches){System.out.println(tweet.toString());}

                if (!matches.isEmpty()) {
                    return gson.toJson(new Resp(SUCCESS, gson.toJson(matches)));
                }else {
                    return gson.toJson(new Resp(CLIENT_ERROR + 4, "Tweet not found"));
                }
            });

            // Websocket modification: Changes the filter of the web socket.
            post("/:filter", (request, response) -> { // /location=Awesomeville&tag=Art&mention=Trees
                logger.info("Post request with filter -> set filter to web socket handler.");
                Map<String, String> params = getQueryMap(request.params(":filter"));
                String location = (String)params.get("location");
                String tag = (String)params.get("tag");
                String mention = (String)params.get("mention");
                logger.info("queryParamsMap.keys() = "+params.keySet() + "\nqueryParamsMap.values() = "+params.values());
                logger.info("loc : "+location+" , tag : "+tag+" , mention : "+mention);
                wsh.setLocation(location);
                wsh.setTag(tag);
                wsh.setMention(mention);
//                init();
                if(!called) {
                    Thread t = new Thread(() -> {
                        consumerWS.poll(Duration.ofMillis(100));
                        Set<TopicPartition> assignment = consumerWS.assignment();
                        consumerWS.seekToBeginning(assignment);
                        while (true) {
                            logger.info("WS polling...");
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
                called = true;

                logger.info("Launched websocket with filters, location : "+wsh.getLocation()+
                        " , tag : "+wsh.getTag()+ " , mention : "+wsh.getMention() +". Redirecting response.");
//                response.redirect("ws://localhost:4242/ws");
//                return response;
                return gson.toJson(new Resp(SUCCESS, "Starting websocket. Visit ws://localhost:4242/ws"));
            });


        });


    }

    // Iterate over all tweets and if one of them has a matching arg, add it to the result.
    private static List<Tweet> matchmaker(HashMap<String, Tweet> tweets, List<String> tweet_ids, String location,
                                    String tag, String mention) {
        List<Tweet> result = new ArrayList<Tweet>();
        if (!tweets.isEmpty()) {
            for (int i = 0; i < tweets.size(); i++) {
                if (tweets.get(tweet_ids.get(i)).filterLoc(location) || tweets.get(tweet_ids.get(i)).filterTag(tag) ||
                        tweets.get(tweet_ids.get(i)).filterMention(mention)) {
                    result.add(tweets.get(tweet_ids.get(i))); // spaghetti code = very much
//                    logger.info("Found filter matching to tweet with id = " + i);
                }
            }
        }
//        if (result.isEmpty()){logger.info("No matching tweets.");}
//        else{logger.info("Total size of matching tweets = "+(result.size()+1));}
        return result;
    }

    private static Map<String, String> getQueryMap(String query)
    {
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


