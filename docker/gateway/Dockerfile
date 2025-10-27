# ------------------------------------------------------------
# 🏗️  BUILD STAGE
# ------------------------------------------------------------
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# Copie des fichiers nécessaires à Gradle
COPY ../../gateway/gradle gradle
COPY ../../gateway/gradlew .
COPY ../../gateway/build.gradle.kts .
COPY ../../gateway/settings.gradle.kts .
COPY ../../gateway/src src

# Build de l’application (fichier JAR)
RUN ./gradlew clean build -x test

# ------------------------------------------------------------
# 🚀  RUNTIME STAGE
# ------------------------------------------------------------
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Copie du jar généré depuis le build précédent
COPY --from=build /app/build/libs/*.jar app.jar

# Port exposé (par défaut 8080)
EXPOSE 8080

# Commande de démarrage
ENTRYPOINT ["java", "-jar", "app.jar"]