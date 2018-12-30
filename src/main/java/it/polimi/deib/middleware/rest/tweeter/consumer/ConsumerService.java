package it.polimi.deib.middleware.rest.tweeter.consumer;

import it.polimi.deib.middleware.rest.commons2.AbstractService;
import it.polimi.deib.middleware.rest.commons2.Resp;
import it.polimi.deib.middleware.rest.commons2.resources.Resource;
import it.polimi.deib.middleware.rest.commons2.resources.serizalization.ResourceDeserializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;

import static spark.Spark.*;

public class ConsumerService extends AbstractService{
    private static KafkaConsumer<String, Resource> consumer;

    private static Map<String, Resource> resources = new HashMap<>();


    public static void main(String[] args) {

        logger = LoggerFactory.getLogger(ConsumerService.class);

        Properties props = new Properties();

        props.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer" + UUID.randomUUID().toString());
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ResourceDeserializer.class);

        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList("tweets1"));
        // get /tweets/filter
        port(3939);
        consumer.poll(Duration.ofMillis(100));
        Set<TopicPartition> assignment = consumer.assignment();

        path("/tweets", () -> {
            before("/*", (q, a) -> logger.info("Consumer received api call"));

            get("/:filter", (request, response) -> {
                String filter = request.params(":filter"); // location=X, tag=X, mention=X
                System.out.print("Get request with filter = " + filter);
                if (resources.containsKey(filter)) { // if filter is legit
                    return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.get(filter))));

                }
                return gson.toJson(new Resp(CLIENT_ERROR + 4, "Resource not found"));
            });

            get("", (request, response) -> {
                System.out.print("get req with no args");
                response.type("application/json");
                response.status(SUCCESS);
//                return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.values())));
                return gson.toJson(new Resp(SUCCESS, gson.toJson(resources.values())));
            });

        });

        Thread t = new Thread(() -> {
            consumer.seekToBeginning(assignment);
            while (true) {
                logger.info("Polling...");
                poll();
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
            }
        });
        t.start();

    }

    private static void poll() {
        ConsumerRecords<String, Resource> tweets = consumer.poll(Duration.ofMillis(500));
        if (!tweets.isEmpty()) {System.out.print("new tweeet\n");}
        tweets.forEach(pr -> resources.put(pr.key(), pr.value()));
        System.out.print(resources.values().size());
    }
}
