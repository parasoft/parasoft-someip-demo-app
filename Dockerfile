FROM azul/zulu-openjdk:17.0.14-17.56

WORKDIR /usr/local/parasoft/demo

COPY ./target/parasoft-someip-demo-app-*.jar ./parasoft-someip-demo-app.jar

EXPOSE 9998 61616

ENTRYPOINT ["java", "-jar", "parasoft-someip-demo-app.jar"]