FROM eclipse-temurin:17-jdk-jammy

WORKDIR /app

COPY src ./src
COPY web ./web
COPY data ./data

RUN find src -name "*.java" > sources.txt && javac -d out @sources.txt

EXPOSE 8080

CMD ["java", "-cp", "out", "skillswap.Main"]
