package it.polimi.deib.middleware.rest.tweeter;

import it.polimi.deib.middleware.rest.commons2.AbstractService;
import it.polimi.deib.middleware.rest.commons2.Resp;
import it.polimi.deib.middleware.rest.commons2.resources.Resource;
import it.polimi.deib.middleware.rest.commons2.resources.serizalization.ResourceDeserializer;
import it.polimi.deib.middleware.rest.commons2.resources.serizalization.ResourceSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.regex.Pattern;

import static spark.Spark.*;


public class AllInOne extends AbstractService {
    private static final String topic = "tweets1";
    private static KafkaProducer<String, Resource> producer;
    private static KafkaConsumer<String, Resource> consumer;
    private static KafkaConsumer<String, Resource> consumerWS;
    private static HashMap<String, Resource> resources = new HashMap<>();


    public static void main(String[] args) {
        port(4242);
        logger = LoggerFactory.getLogger(AllInOne.class);

        // Producer
        Properties propsProd = new Properties();
        propsProd.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propsProd.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        propsProd.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ResourceSerializer.class.getName());
        producer = new KafkaProducer<>(propsProd);

        // Consumer
        Properties propsCons = new Properties();
        propsCons.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer" + UUID.randomUUID().toString());
        propsCons.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        propsCons.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propsCons.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsCons.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResourceDeserializer.class);
        consumer = new KafkaConsumer<>(propsCons);
        consumer.subscribe(Collections.singletonList("tweets1"));

        // Consumer using webSocket
        consumerWS = new KafkaConsumer<>(propsCons);
        consumerWS.subscribe(Collections.singletonList("tweets1"));
        WebSocketHandler wsh = new WebSocketHandler(consumerWS);
        webSocket("/tweets/:filter", wsh);
        init();

        // REST UTILITY
        path("/tweets", () -> {
            before("/*", (q, a) -> logger.info("Received api call to /tweets"));

            get("/:filter", (request, response) -> { // /location=Awesomeville&tag=Art&mention=Trees
                String filter = request.params(":filter");
                System.out.println("Get request with filter = " + filter);
                Map<String, String> params = getQueryMap(filter);
                System.out.println("queryParamsMap.keys() = "+params.keySet() + "\nqueryParamsMap.values() = "+params.values());
                String location = (String)params.get("location");
                String tag = (String)params.get("tag");
                String mention = (String)params.get("mention");
                System.out.println(location);
                System.out.println(tag);
                System.out.println(mention);


                if (resources.containsKey(filter)) { // if filter is legit
                    return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.get(filter))));
                }else {
                    return gson.toJson(new Resp(CLIENT_ERROR + 4, "Resource not found"));
                }
            });

            get("", (request, response) -> {
                System.out.print("get req with no args\n");
                response.type("application/json");
                response.status(SUCCESS);
//                return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.values())));
                return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.values())));
            });

            post("", (request, response) -> { // user posted a new tweet


                response.type("application/json");
                response.status(SUCCESS);
                String body = request.body();
                if (body != null && !body.isEmpty()) { // optionally regex type checking
                    String id = tweet(gson.fromJson(body, Resource.class));
                    return gson.toJson(new Resp(SUCCESS, "Resource Created with id [" + id + "]"));
                } else
                    return gson.toJson(new Resp(400, "Bad Request"));
            });

        });

        // Start consumer polling
        System.out.println("b4");
        Thread t = new Thread(() -> {
            consumer.poll(Duration.ofMillis(100));
            Set<TopicPartition> assignment = consumer.assignment();
            consumer.seekToBeginning(assignment);
            while (true) {
                logger.info("Polling...");
                wsh.poll();
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


    // Pushes tweet to kafka.
    public static String tweet(Resource message) {
        String s = UUID.randomUUID().toString().split("-")[0];
        message.setId(s);
        ProducerRecord<String, Resource> record = new ProducerRecord<>(topic, s, message);
        producer.send(record);
        return s;
    }

    // Polling the consumer to see if new tweets are present.
    private static void poll() {
        ConsumerRecords<String, Resource> newTweets = consumer.poll(Duration.ofMillis(500));
        newTweets.forEach(pr -> resources.put(pr.key(), pr.value()));
        System.out.println(resources.values());
//        printMap(resources);
    }

    public static void printMap(Map mp) {
        Iterator it = mp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            System.out.println("pair.getKey(): "+ pair.getKey() +" pair.getValue(): " + pair.getValue()); // key is given in value's id
            it.remove(); // avoids a ConcurrentModificationException
        }
    }

    public static Map<String, String> getQueryMap(String query)
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

    @WebSocket
    public static class WebSocketHandler {

        private final KafkaConsumer<String, Resource> consumerWS;
        List<Session> users = new ArrayList<>();

        public WebSocketHandler(KafkaConsumer<String, Resource> consumerWS) {
            this.consumerWS = consumerWS;
        }

        public void poll() {
            ConsumerRecords<String, Resource> tweets = this.consumerWS.poll(Duration.ofMillis(500));
            System.out.println("Called WS poll");
            tweets.forEach(pr -> this.broadcast(gson.toJson(pr.value())));
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
}

//Client  GET (manual) || Stream continuous (timestamp < 5mins)
//        Client  Subscribe to user ID  POST /users/id
//        Write consumer index on client disk (cf ex)
