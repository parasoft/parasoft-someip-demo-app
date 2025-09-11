FROM azul/zulu-openjdk:17.0.14-17.56

WORKDIR /usr/local/parasoft/demo

COPY ./target/demo-soavirt-someip-*.jar ./demo-soavirt-someip.jar

EXPOSE 9998 61616

ENTRYPOINT ["java", "-jar", "demo-soavirt-someip.jar"]