# Kafka Rest Tweeter Homework Repository for the MiddleWare Course


Requests: below for unix. For windows use postman app.

For websocket testing consider Chrome plugin Smart Websocket Client.

REST API:

Subscribe to twitter - POST /users/id = make server bind a kafka consumer to tweets1 topic \
new user can register using that call.. \ 
Write a new tweet - POST /tweets JSON Body \
Read tweets (polling) - GET /tweets/{filter}/latest \
Server-Sent Event (streaming) - POST /tweets/{filter} JSONBody [List of tweets]

Requirements:

Exactly once semantics: a tweet is always stored and presented in timelines exactly once. \
Upon restart/failure clients should not read all the posts from scratch.

```bash

-- Subscribe a new user
curl -X POST \
  http://localhost:4242/users/bobross \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json'
 
-- Post a new tweet
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
    "message": "Resource Created with id [ed96c993]"
}
 
-- Read tweets (manual GET)
curl -X GET \
  http://localhost:4242/tweets/location=Awesomeville&tag=Art&mention=Trees
  
-- WebSocket
curl -X POST \
  http://localhost:4242/tweets/location=Awesomeville&tag=Art&mention=Trees \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json'
  


bin\windows\zookeeper-server-start.bat config\zookeeper.properties

bin\windows\kafka-server-start.bat config\server.properties

bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 2 --partitions 1 --topic tweeter1

bin\windows\kafka-console-producer.bat --broker-list localhost:9092 --topic tweeter1

bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic tweeter1 --from-beginning

bin\windows\kafka-topics.bat --describe --zookeeper localhost:2181 --topic tweeter1





```
