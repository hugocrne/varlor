# ------------------------------------------------------------
# 🏗️  BUILD STAGE
# ------------------------------------------------------------
FROM mcr.microsoft.com/dotnet/aspnet:9.0 AS base
WORKDIR /app
EXPOSE 80
EXPOSE 443

FROM mcr.microsoft.com/dotnet/sdk:9.0 AS build
WORKDIR /src

# Copie des fichiers nécessaires au build
COPY ["product-api/product-api.csproj", "product-api/"]
RUN dotnet restore "product-api/product-api.csproj"

# Copie du reste des sources
COPY . .
WORKDIR "/src/product-api"
RUN dotnet build "product-api.csproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "product-api.csproj" -c Release -o /app/publish /p:UseAppHost=false

# ------------------------------------------------------------
# 🚀  RUNTIME STAGE
# ------------------------------------------------------------
FROM base AS final
WORKDIR /app

# Copie de l'application publiée
COPY --from=publish /app/publish .

# Configuration des variables d'environnement
ENV ASPNETCORE_ENVIRONMENT=Production
ENV ASPNETCORE_URLS=http://+:80

# Port exposé
EXPOSE 80

# Commande de démarrage
ENTRYPOINT ["dotnet", "product-api.dll"]