# Kibana Saved Objects
### Create data view

- Open Kibana at http://localhost:5601
- Go to Stack Management → Data Views
- Click Create data view
- Name: leaky-tokens
- Index pattern: leaky-tokens-*
- Time field: @timestamp (or event.@timestamp if you use the target => "event" codec)
Save