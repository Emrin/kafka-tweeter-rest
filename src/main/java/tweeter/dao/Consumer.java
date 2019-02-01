package tweeter.dao;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tweeter.resources.Tweet;
import tweeter.resources.serizalization.TweetDeserializer;

import java.time.Duration;
import java.util.*;

public class Consumer {
    // Args
    private Logger logger = LoggerFactory.getLogger(Consumer.class);
    private KafkaConsumer<String, Tweet> consumer;
    private HashMap<String, Tweet> tweetsMap = new HashMap<>();
    private List<String> tweetIds = new ArrayList<>();

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

        Set<TopicPartition> assignment = consumer.assignment();
        consumer.seekToBeginning(assignment);
        consumer.poll(Duration.ofMillis(100));

        Thread t = new Thread(() -> {
            while (true) {
                logger.info("Polling tweets...");
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

    // Methods

    public HashMap<String, Tweet> getTweetsMapMap(){
        return tweetsMap;
    }

    public List<String> getTweetIds(){
        return tweetIds;
    }

    // Polling the consumer to see if new tweets are present.
    public void poll() {
        ConsumerRecords<String, Tweet> newTweets = consumer.poll(Duration.ofMillis(500));
        for (ConsumerRecord<String, Tweet> tweet : newTweets){
            tweetsMap.put(tweet.key(), tweet.value());
            tweetIds.add(tweet.key());
        }
        System.out.println("Polled "+newTweets.count()+" new tweets.");
    }

    public void pollInitial() {
        Set<TopicPartition> assignment = consumer.assignment();
        consumer.seekToBeginning(assignment);
        ConsumerRecords<String, Tweet> newTweets = consumer.poll(Duration.ofMillis(100));
        for (ConsumerRecord<String, Tweet> tweet : newTweets){
            System.out.println("Adding saved tweet.");
            tweetsMap.put(tweet.key(), tweet.value());
            tweetIds.add(tweet.key());
        }
        System.out.println("Polled "+newTweets.count()+" new tweets.");
    }

}