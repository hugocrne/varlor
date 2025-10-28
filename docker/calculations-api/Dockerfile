# =========================
# 🧱 Stage 1: Build
# =========================
FROM ubuntu:24.04 AS build

RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential cmake ninja-build git clang curl ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /opt

# 🧩 Installer Oat++ v1.3.0
RUN git clone --branch 1.3.0 --depth=1 https://github.com/oatpp/oatpp.git
WORKDIR /opt/oatpp/build

# ⚙️ Compiler Oat++
RUN cmake -G Ninja -DCMAKE_BUILD_TYPE=Release \
    -DCMAKE_INSTALL_PREFIX=/usr/local \
    -DCMAKE_INSTALL_LIBDIR=lib \
    .. && ninja && ninja install

# 🧠 Copier tous les headers nécessaires (Oat++ 1.3.0 ne les copie pas tous)
RUN mkdir -p /usr/local/include && \
    cp -r /opt/oatpp/src/* /usr/local/include/ && \
    ldconfig

# ✅ Vérifications
RUN echo "📂 Vérif headers oatpp :" && find /usr/local/include -name "component.hpp" | head -3 && \
    echo "📦 Vérif lib oatpp :" && find /usr/local/lib -type f -name "liboatpp.a"

# 🧱 Compilation du projet
WORKDIR /app
COPY calculations-api /app

RUN rm -rf /app/build && \
    cmake -G Ninja \
    -DCMAKE_BUILD_TYPE=Release \
    -S . -B build && ninja -C build

# =========================
# 🚀 Stage 2: Runtime
# =========================
FROM ubuntu:24.04 AS runtime

RUN apt-get update && apt-get install -y --no-install-recommends \
    libstdc++6 ca-certificates && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=build /app/build/calculations_api /app/calculations_api

EXPOSE 8000
CMD ["./calculations_api"]