# Kafka Rest Tweeter Homework Repository for the MiddleWare Course

TODO : see 09-bigdata 67 for Kstream->stream()->forEach->websock.broadcast()

Requests: below for unix. For windows use postman app.

For websocket testing consider Chrome plugin Smart Websocket Client.

REST API:

Subscribe to twitter - POST /users/id \
new user can register using that call.. \ 
Write a new tweet - POST /tweets JSON Body \
Read tweets (polling) - GET /tweets/{filter}/latest \
Server-Sent Event (streaming) - POST /tweets/{filter} JSONBody [List of tweets]

Requirements:

Exactly once semantics: a tweet is always stored and presented in timelines exactly once. \
Upon restart/failure clients should not read all the posts from scratch.

```bash

bin\windows\zookeeper-server-start.bat config\zookeeper.properties

bin\windows\kafka-server-start.bat config\server.properties

for 2nd broker duplicate properties file and change:
cp config/server.properties config/server-1.properties
config/server-1.properties:
    broker.id=1
    listeners=PLAINTEXT://:9093
    log.dirs=/tmp/kafka-logs-1
 
bin\windows\kafka-server-start.bat config\server-1.properties

bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 2 --partitions 1 --topic tweeter2

bin\windows\kafka-console-producer.bat --broker-list localhost:9092 --topic tweeter2

bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic tweeter2 --from-beginning

bin\windows\kafka-topics.bat --describe --zookeeper localhost:2181 --topic tweeter2

```

```bash

1 -- Subscribe a new user

 
2 -- Post a new tweet
curl -X POST \
  http://localhost:4242/tweets \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -d '{	"author" : "Bob",
      	"location" : "Florida",
      	"tags": [
      		"happy", "trees"
      		],
      	"mentions" : [
      		"@art", "@painting"
      		]
      }'
      
Response:
{
    "status": 200,
    "message": "Tweet Created: [id = d5995e64 ; author = Bob ; location = Florida ; tags = [happy, trees] ; mentions = [@art, @painting]]"
}
 
3 -- Read tweets (manual GET)
curl -X GET \
  http://localhost:4242/tweets/location=Awesomeville&tag=Art&mention=Trees
  
-- WebSocket (Stream)
curl -X POST \
  http://localhost:4242/tweets/location=Awesomeville&tag=Art&mention=Trees \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json'
  
```

