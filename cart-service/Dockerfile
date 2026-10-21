FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN ./mvnw -pl common-dto,cart-service clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/cart-service/target/cart-service-1.0.0.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]
