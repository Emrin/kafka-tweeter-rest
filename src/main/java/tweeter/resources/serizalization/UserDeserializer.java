package tweeter.resources.serizalization;

import org.apache.kafka.common.serialization.Deserializer;
import org.codehaus.jackson.map.ObjectMapper;
import tweeter.resources.Tweet;
import tweeter.resources.User;

import java.util.Map;

public class UserDeserializer implements Deserializer<User> {
    @Override
    public void configure(Map configs, boolean isKey) {

    }

    @Override
    public User deserialize(String topic, byte[] data) {
        ObjectMapper mapper = new ObjectMapper();
        User request = null;
        try {
            request = mapper.readValue(data, User.class);
        } catch (Exception e) {

            e.printStackTrace();
        }
        return request;
    }

    @Override
    public void close() {

    }
}
