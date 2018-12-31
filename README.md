# Kafka Rest Tweeter Homework Repository for the MiddleWare Course


Requests for unix, for windows use postman app.

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
  



```
