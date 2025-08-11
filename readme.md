# README
Man får en del nyttig info når man starter elastic podden.
Blant annet passord, Sha fingerprint til sertifikatet, noe som heter 'enrollment-token'


Så skal vi på java siden gjøre noe greier
Vi oppretter client-config


Elastic fungerer som ett REST-api


1.  Starter Containerrenen
    docker run -d --name es01 --net elastic -p 9200:9200 -it -m 2GB docker.elastic.co/elasticsearch/elasticsearch:9.0.0

2. Kopierer http-cert kan være greit
   `docker cp es01:/usr/share/elasticsearch/config/certs/http_ca.crt .`

3. Hent ut pw
   `docker logs 48e3b7c38d92 | grep -i -A20 -B8  'password'`

4. Få ut Sha-fingerprint
   `openssl x509 -in http_ca.crt -noout -fingerprint -sha256`

5. Kjør kibana
   `docker run -d --name kib01 --net elastic -p 5601:5601 docker.elastic.co/kibana/kibana:9.0.0`

6. Sjekk loggene, trykk på linken
   `docker logs  134daa9c5663`

Opprett API key
Elastic search -> endpoints & api-keys -> create key

For å laste ICES feature-collection opp måtte jeg lage ett script som rettet filen
Var problem med 27.2.a.2 i at den ikke fulgte RHR  
**Right-Hand Rule**
* Exterior rings (outer boundaries) must be counter-clockwise
* Interior rings (holes) must be clockwise

Var og noen andre problemer med filen, muligens overlappende koordinater. Elasticsearch bruker noe som heter z-curve encoding som krasjet på polygonet, trolig grunnet deeply nested, malformed, or overlapping MultiPolygons
Kjør appen fix-geojson på slike filer
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

VIKTIG, lag en index

Nå som vi har en kjørende elasticsearch-container bør vi opprette en index.
```bash
curl -X PUT --cacert http_ca.crt https://localhost:9200/soner \
  -H 'Authorization: ApiKey NFZrRExKZ0JfUXp0YThFeGhKcVI6c1NveUJhWFFPOE0zcVdIcG9rZmdHZw==' \
  -H 'Content-Type: application/json' \
  -d '{
  "mappings": {
    "properties": {
      "geometry": {
        "type": "geo_shape"
      },
      "properties": {
        "type": "object"
      }
    }
  }
}'
```

Når har lastet opp alle ices-områdene, kan vi søke
```bash
curl -X POST --cacert http_ca.crt https://localhost:9200/multiupload/_search \
        -H 'Authorization: ApiKey NFZrRExKZ0JfUXp0YThFeGhKcVI6c1NveUJhWFFPOE0zcVdIcG9rZmdHZw==' \
        -H 'Content-Type: application/json' \
        -d '{
  "query": { "match" : { "properties.FID": 1 } },
  "_source": false,
  "fields": [
    "id",
    "properties.*"
  ]
}' | jq 
```
Lag en api-key for index "soner"  
``` bash
curl -X POST "https://localhost:9200/_security/api_key" \
  -H "Content-Type: application/json" \
  -u elastic:$ELASTIC_PASSWORD \
  --cacert http_ca.crt \
  -d '{
    "name": "my-api-key",
    "expiration": "1d",
    "role_descriptors": {
      "custom-role": {
        "cluster": ["all"],
        "index": [
          {
            "names": ["*"],
            "privileges": ["all"]
          }
        ]
      }
    }
  }'
```

``` bash
curl --cacert http_ca.crt -u elastic:$ELASTIC_PASSWORD https://localhost:9200
```


curl -X POST --cacert http_ca.crt https://localhost:9200/ezy/_search \
-H 'Authorization: ApiKey aUhDZGpKY0JoN1BKMHJYUEpLd3g6aGdiQ0gydXhmc0pqWkU4SEVYTTZ6Zw==' \
-H 'Content-Type: application/json' \
-d '{
"query": { "match" : { "_id": "jHDIjJcBh7PJ0rXPe6xu" } },
"_source": false,
"fields": [
"id",
"properties"
]
}'

curl -X POST --cacert http_ca.crt https://localhost:9200/ezy/_search \
-H 'Authorization: ApiKey aUhDZGpKY0JoN1BKMHJYUEpLd3g6aGdiQ0gydXhmc0pqWkU4SEVYTTZ6Zw==' \
-H 'Content-Type: application/json' \
-d '{"query":{"bool":{"filter":{"geo_shape":{"geometry":{"relation":"contains","shape":{"coordinates":[9.3013,57.3239],"type":"point"}}}},"must":{"match_all":{}}}}}'

curl -X POST --cacert http_ca.crt https://localhost:9200/ezy/_search \
-H 'Authorization: ApiKey aUhDZGpKY0JoN1BKMHJYUEpLd3g6aGdiQ0gydXhmc0pqWkU4SEVYTTZ6Zw==' \
-H 'Content-Type: application/json' \
-d '{"query":{"bool":{"filter":{"geo_shape":{"geometry":{"relation":"contains","shape":{"coordinates":[9.3013,57.3239],"type":"point"}}}},"must":{"match_all":{}}}}}'

-- Inkluderer bare ønskede felter, effektivt over nettet, men ikke den mest moderne måten
curl -X POST --cacert http_ca.crt https://localhost:9200/ezy/_search \
-H 'Authorization: ApiKey aUhDZGpKY0JoN1BKMHJYUEpLd3g6aGdiQ0gydXhmc0pqWkU4SEVYTTZ6Zw==' \
-H 'Content-Type: application/json' \
-d '{"query":{"bool":{"filter":{"geo_shape":{"geometry":{"relation":"contains","shape":{"coordinates":[9.3013,57.3239],"type":"point"}}}},"must":{"match_all":{}}}},"_source":{"includes":["properties.name","properties.FID"]}}'



curl -X POST --cacert http_ca.crt https://localhost:9200/ezy/_search \
-H 'Authorization: ApiKey aUhDZGpKY0JoN1BKMHJYUEpLd3g6aGdiQ0gydXhmc0pqWkU4SEVYTTZ6Zw==' \
-H 'Content-Type: application/json' \
-d '{"query":{"bool":{"filter":{"geo_shape":{"geometry":{"relation":"contains","shape":{"coordinates":[9.3013,57.3239],"type":"point"}}}},"must":{"match_all":{}}}},"_source":false,"fields":["properties.name","properties.FID"]}'

curl -X POST --cacert http_ca.crt https://localhost:9200/ezy/_search \
-H 'Authorization: ApiKey aUhDZGpKY0JoN1BKMHJYUEpLd3g6aGdiQ0gydXhmc0pqWkU4SEVYTTZ6Zw==' \
-H 'Content-Type: application/json' \
-d '{
"query": { "match" : { "id": 0 } }}'




curl --cacert http_ca.crt "https://localhost:9200/my-sones/_doc" \
-H "Content-Type: application/json" \
--header 'Authorization: ApiKey aUhDZGpKY0JoN1BKMHJYUEpLd3g6aGdiQ0gydXhmc0pqWkU4SEVYTTZ6Zw==' \
--data-binary @EEZ_Denmark.geojson

curl -X GET --cacert http_ca.crt https://localhost:9200/ezy/_search?pretty --header 'Authorization: ApiKey aUhDZGpKY0JoN1BKMHJYUEpLd3g6aGdiQ0gydXhmc0pqWkU4SEVYTTZ6Zw=='


curl -X 'GET' \
'https://apps.fiskeridirektoratet.no/zonereg/api/v1/zones?latitude=57.3123&longitude=9.3123&buffer=120' \
-H 'accept: */*' \
-w "\nTotal time: %{time_total} seconds\n"



curl -X 'GET' \
'https://apps.fiskeridirektoratet.no/zonereg/api/v1/zones?latitude=57.3123&longitude=9.3123&buffer=120' \
-H 'accept: */*' \
-w "\nTotal time: %{time_total} seconds\n


{"query":{"bool":{"filter":{"geo_shape":{"geometry":{"relation":"contains","shape":{"coordinates":[9.3013,57.3239],"type":"point"}}}},"must":{"match_all":{}}}},"_source":false,"fields":["properties.name","properties.FID"]}


{
"bool": {
"filter": {
"geo_shape": {
"geometry": {
"relation": "contains",
"shape": {
"coordinates": [
9.3013,
57.3239
],
"type": "point"
}
}
}
},
"must": {
"match_all": {}
}
}
}

{"query":{"bool":{"filter":{"geo_shape":{"geometry":{"relation":"contains","shape":{"coordinates":[9.3013,57.3239],"type":"point"}}}},"must":{"match_all":{}}}},"_source":{"includes":["properties.name","properties.FID"]}}




curl -X GET --cacert http_ca.crt https://localhost:9200/shiz/_mapping \
-H 'Authorization: ApiKey NFZrRExKZ0JfUXp0YThFeGhKcVI6c1NveUJhWFFPOE0zcVdIcG9rZmdHZw==' 



