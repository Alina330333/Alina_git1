FROM openjdk:17-slim
WORKDIR /app
COPY src/adalexer/*.java ./adalexer/
RUN javac adalexer/*.java
ENTRYPOINT ["java", "-cp", ".", "adalexer.Main"]