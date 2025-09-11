# Parasoft SOME/IP demo app

## Requirements
- Java 17+

## How to build and run project

```shell
  mvn clean package
  java -jar target/soavirt-someip-demo-***.jar
```

## How to build and run docker image

```shell
  docker build -t parasoft-demo-app:latest .
  docker run -d -p 9998:9998 -p 61616:61616 --name parasoft-demo-app parasoft-demo-app:latest
```

## Default settings

| Property          | Default Value         |
|-------------------|-----------------------|
| Application Url   | http://localhost:9998 |
| Message Queue Url | tcp://0.0.0.0:61616   |
| Queue name        | someip_message_queue  |
