# Kafka Rest Tweeter Homework Repository for the MiddleWare Course

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

bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 2 --partitions 1 --topic tweeter3
bin\windows\kafka-topics.bat --create --zookeeper localhost:2181 --replication-factor 2 --partitions 1 --topic users2

bin\windows\kafka-console-producer.bat --broker-list localhost:9092 --topic tweeter2

bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic tweeter3 --from-beginning
bin\windows\kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic users2 --from-beginning

bin\windows\kafka-topics.bat --describe --zookeeper localhost:2181 --topic tweeter2

```

```bash

1 -- Subscribe to tweeter

So a new user will POST /users/hisName
And push himself into kafka users topic if he's not there.

curl -X POST \
  http://localhost:4242/users/wick \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -d '{
      "fullname" : "John Wick",
      "email" : "john@x.com",
      "age" : "25"
      }'

-- Response
{
    "status":200,
    "message":"New user added: 
    {
    \"fullname\":\"John Wick\",
    \"id\":\"wick\",
    \"email\":\"wick@x.com\",
    \"age\":\"25\"
    }"
}

Second time:
{"status":409,"message":"User already exists."}

-- In kafka topic:
{
    "id":"wick","fullname":"John Wick","email":"john@x.com","age":"25"
}

2 -- Post a new tweet
Post the tweet if author is in users topic only.

curl -X POST \
  http://localhost:4242/tweets \
  -H 'Cache-Control: no-cache' \
  -H 'Content-Type: application/json' \
  -d '{	"author" : "wick",
        "location" : "Hell",
        "tags": [
                "token", "things"
                ],
        "mentions" : [
                "@award", "@chase"
                ]
      }'

-- Response
{
    "status": 200,
    "message": "Tweet Created: [id = 91d500ba ; author = wick ; location = Hell ; tags = [token, things] ; mentions = [@award, @chase]]"
}

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
      
-- Response:
{
    "status": 409,
    "message": "Author doesn't exist."
}
 
3 -- Read tweets (manual GET)
Filter can be in any order, as long as structure is respected.

curl -X GET \
    localhost:4242/tweets/location=hell

-- Response:
{
    "status":200,
    "message":"[{\"id\":\"bbd2cee6\",\"author\":\"wick\",\"location\":\"Hell\",\"tags\":[\"token\",\"things\"],\"mentions\":[\"@award\",\"@chase\"]}]"
}

curl -X GET \
    localhost:4242/tweets/location=unknown

-- Response:
{
    "status":404,
    "message":"Tweet not found"
}

curl -X GET \
    localhost:4242/tweets/mention=space

{
    "status":200,
    "message":"[{\"id\":\"663a7bd9\",\"author\":\"rick\",\"location\":\"Space\",\"tags\":[\"interdimensional\",\"cable\"],\"mentions\":[\"@space\",\"@time\"]}]"
}
  
4 -- WebSocket + Kafka Stream
Not sure about the requirement here.
Streaming is done with a websocket.
And to connect to a websocket you need to visit ws://localhost

Connect to websocket with this:
ws://localhost:4242/ws
(use Smart Websocket Client from Chrome)

To change your session filter just send
a message such as tags=candy&mentions=rabbit
through the websocket.

On connection, user gets all tweets.
When he sends a filter, he gets all corresponding tweets.
Once a tweet is published, he gets it if it passes his filter.


```

