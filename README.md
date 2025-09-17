# Parasoft SOME/IP Demo Application
The Parasoft SOME/IP Demo Application provides a web-based UI to visualize SOME/IP communication messages between simulated ECUs (ACC, TSR, Radar, Google Map) and a real PPDM ECU.

The real PPDM ECU is sourced from the [parasoft-autosar-demo](https://github.com/parasoft-autosar-demo/autosar_demo) project.

The simulated ECUs are simulated by soavirt-automotive project. 

**ECU Responsibilities and Workflow**

- Radar ECU

  Publishes the distance and relative speed of vehicles ahead.


- TSR ECU

  Provides the speed limit detected from current traffic signs.


- Google Map ECU

  Provides the speed limit of the current road.


- ACC ECU

  Requests the PPDM ECU to get the maximum vehicle speed that needs to be controlled.


- PPDM ECU

  Upon receiving a request from the ACC ECU, it performs the following operations:
  - Receives distance and relative speed data published by the Radar ECU.
  - Requests the TSR ECU for the speed limit detected from traffic signs.
  - Requests the Google Map ECU for the current road speed limit.
  - Calculates the maximum vehicle speed to be controlled based on the collected data.
  - Returns the calculated maximum speed to the ACC ECU.

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

The application embeds an ActiveMQ server to receive SOME/IP messages sent from simulated ECUs, all messages are sent to the **_someip_message_queue_** queue.