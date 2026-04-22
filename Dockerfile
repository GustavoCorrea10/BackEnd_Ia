# ESTÁGIO 1: Compilação (Build)
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia apenas o pom.xml primeiro para aproveitar o cache das dependências
COPY pom.xml .
COPY src ./src

# Compila o projeto ignorando a compilação de testes
RUN mvn clean package -Dmaven.test.skip=true

# ESTÁGIO 2: Execução (Run)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Copia o JAR gerado no estágio anterior com um nome fixo e simples
COPY --from=build /app/target/assistente-0.0.1-SNAPSHOT.jar app.jar

# Expõe a porta (padrão Spring Boot)
EXPOSE 8080

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]