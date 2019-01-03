package tweeter;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Session;
import tweeter.api.commons.AbstractService;
import tweeter.api.commons.Resp;
import tweeter.dao.Consumer;
import tweeter.dao.Producer;
import tweeter.dao.WebSocketHandler;
import tweeter.resources.Tweet;
import tweeter.resources.serizalization.TweetDeserializer;

import java.time.Duration;
import java.util.*;

import static spark.Spark.*;


public class TweeterApp extends AbstractService {

    static Boolean called = false;

    public static void main(String[] args) {

        Logger logger = LoggerFactory.getLogger(TweeterApp.class);
        String topic = "tweets1";
        int portNum = 4242;
        HashMap<String, Tweet> tweets = new HashMap<>();
        List<Session> users = new ArrayList<>();

        port(portNum);

        // Producer
        Producer producer = new Producer(topic);

        // Consumer
        Consumer consumer = new Consumer(topic);

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


        path("/tweets", () -> {
            before("/*", (q, a) -> logger.info("Received api call to /tweets"));

            // User GET request.
            get("/:filter", (request, response) -> { // /location=Awesomeville&tag=Art&mention=Trees
                System.out.println("Get request with filter.");
                Map<String, String> params = getQueryMap(request.params(":filter"));
                String location = (String)params.get("location");
                String tag = (String)params.get("tag");
                String mention = (String)params.get("mention");
                System.out.println("queryParamsMap.keys() = "+params.keySet() + "\nqueryParamsMap.values() = "+params.values());
                System.out.println("loc : "+location+" , tag : "+tag+" , mention : "+mention);

                // Get list of tweets that have matching args.
                List<Tweet> matches = matchmaker(tweets, location, tag, mention);

                if (!matches.isEmpty()) {
                    return gson.toJson(new Resp(SUCCESS, gson.toJson(matches)));
                }else {
                    return gson.toJson(new Resp(CLIENT_ERROR + 4, "Tweet not found"));
                }
            });

            // User posts a tweet.
            post("", (request, response) -> { // User posted a new tweet
                response.type("application/json");
                response.status(SUCCESS);
                String body = request.body();
                try {
                    if (body != null && !body.isEmpty()) { // maybe check form with regex
                        Tweet thisTweet = producer.place(gson.fromJson(request.body(), Tweet.class));
                        return gson.toJson(new Resp(SUCCESS, "Tweet Created: [" + thisTweet.toString() + "]"));
                    } else
                        return gson.toJson(new Resp(400, "Bad Request"));
                } catch (Exception e) {
                    System.out.println("Exception : "+e.toString());
                    return gson.toJson(new Resp(400, "Bad Request"));
                }
            });

            // Websocket call: Changes the filter of the web socket.
            post("/:filter", (request, response) -> { // /location=Awesomeville&tag=Art&mention=Trees
                System.out.println("Post request with filter -> set filter to web socket handler.");
                Map<String, String> params = getQueryMap(request.params(":filter"));
                String location = (String)params.get("location");
                String tag = (String)params.get("tag");
                String mention = (String)params.get("mention");
                System.out.println("queryParamsMap.keys() = "+params.keySet() + "\nqueryParamsMap.values() = "+params.values());
                System.out.println("loc : "+location+" , tag : "+tag+" , mention : "+mention);
                wsh.setLocation(location);
                wsh.setTag(tag);
                wsh.setMention(mention);
//                init();

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
                if(!called) {t.start();}
                called = true;

                System.out.println("Launched websocket with filters, location : "+wsh.getLocation()+
                        " , tag : "+wsh.getTag()+ " , mention : "+wsh.getMention() +". Redirecting response.");
                response.redirect("ws://localhost:4242/ws");
                return response;
//                return gson.toJson(new Resp(SUCCESS, "Starting websocket."));
            });


        });


    }

    // Iterate over all tweets and if one of them has a matching arg, add it to the result.
    private static List<Tweet> matchmaker(HashMap<String, Tweet> tweets, String location,
                                    String tag, String mention) {
        List<Tweet> result = new ArrayList<Tweet>();
        for (int i = 0; i < tweets.size(); i++){
            if (tweets.get(i).filterLoc(location) || tweets.get(i).filterTag(tag) || tweets.get(i).filterTag(mention)){
                result.add(tweets.get(i));
            }
        }
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

