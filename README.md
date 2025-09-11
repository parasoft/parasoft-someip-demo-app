# Parasoft SOME/IP Demo Application
The Parasoft SOME/IP Demo Application is used to demonstrate the speed from simulated ACC, TSR, Radar and Google Map ECUs.

## Requirements
- Java 17+

## Getting Started
### Build .jar from sources
```shell
  mvn clean package
```

### Running
```shell
  java -jar target/parasoft-someip-demo-app-***.jar  --server.port=9998
```
You can visit the application at [http://localhost:9998](http://localhost:9998).

## Docker Image

### Build a Docker Image from Sources
```shell
  docker build -t parasoft-someip-demo-app:latest .
```

### Run Docker Image
```shell
  docker run -d -p 9998:9998 -p 61616:61616 --name parasoft-someip-demo-app parasoft-someip-demo-app:latest
```
You can visit the application at [http://localhost:9998](http://localhost:9998).

## Default settings

| Property                     | Default Value        | Property                   |
|------------------------------|----------------------|----------------------------|
| Application Port             | 9998                 | server.port                |
| Embedded ActiveMQ Server Url | tcp://0.0.0.0:61616  | spring.activemq.broker-url |
| ActiveMQ Queue Name          | someip_message_queue | N.A.                       |
