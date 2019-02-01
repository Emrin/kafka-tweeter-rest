package tweeter.dao;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tweeter.resources.Tweet;
import tweeter.resources.serizalization.TweetDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

public class Consumer {
    // Args
    private Logger logger = LoggerFactory.getLogger(Consumer.class);
    private KafkaConsumer<String, Tweet> consumer;

    // Constructor
    public Consumer(String topic){
        Properties propsCons = new Properties();
        propsCons.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer" + UUID.randomUUID().toString());
        propsCons.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        propsCons.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propsCons.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsCons.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, TweetDeserializer.class);
        consumer = new KafkaConsumer<>(propsCons);
        consumer.subscribe(Collections.singletonList(topic));

        Thread t = new Thread(() -> {
            consumer.poll(Duration.ofMillis(100));
            Set<TopicPartition> assignment = consumer.assignment();
            consumer.seekToBeginning(assignment);
            while (true) {
                logger.info("Polling tweets...");
                poll(consumer);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        t.start();
    }

    // Methods

    // Fill the initial tweets hashmap with previous tweets inside topics.
    public void init() {

    }

    // Polling the consumer to see if new tweets are present.
    public void poll(KafkaConsumer<String, Tweet> consumer) {
        ConsumerRecords<String, Tweet> newTweets = consumer.poll(Duration.ofMillis(500));
        System.out.println("Polled "+newTweets.count()+" new tweets.");
    }


}