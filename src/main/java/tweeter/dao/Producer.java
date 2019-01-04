package tweeter.dao;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tweeter.resources.Tweet;
import tweeter.resources.serizalization.TweetSerializer;

import java.util.Properties;
import java.util.UUID;

public class Producer {
    // Args
    private Logger logger = LoggerFactory.getLogger(Producer.class);
    private KafkaProducer<String, Tweet> producer;
    private final String topic;

    // Constructor
    public Producer(String topic){
        this.topic = topic;

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, TweetSerializer.class.getName());
        this.producer = new KafkaProducer<>(props);
    }

    // Methods
    public Tweet place(Tweet tweet) {
        String s = UUID.randomUUID().toString().split("-")[0];
        tweet.setId(s);
        ProducerRecord<String, Tweet> record = new ProducerRecord<>(this.topic, s, tweet); // topic, id, object
        this.producer.send(record);
        logger.info("Created new record.");
        return tweet;
    }

    public void close() {
        this.producer.close();
    }
}
