# Kafka Rest Tweeter Homework Repository for the MiddleWare Course


Requests for unix, for windows use postman app.

```bash


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





```
