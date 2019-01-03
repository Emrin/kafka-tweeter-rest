package tweeter.dao;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import tweeter.resources.Tweet;
import tweeter.resources.serizalization.TweetDeserializer;

import java.util.Collections;
import java.util.Properties;
import java.util.UUID;

public class Consumer {
    // Args
    private KafkaConsumer<String, Tweet> consumer;
    private final String topic;

    // Constructor
    public Consumer(String topic){
        this.topic = topic;

        Properties propsCons = new Properties();
        propsCons.put(ConsumerConfig.GROUP_ID_CONFIG, "consumer" + UUID.randomUUID().toString());
        propsCons.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        propsCons.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        propsCons.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        propsCons.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, TweetDeserializer.class);
        consumer = new KafkaConsumer<>(propsCons);
        consumer.subscribe(Collections.singletonList(topic));
    }

    // Methods

}