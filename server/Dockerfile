FROM eclipse-temurin:17-jdk-jammy AS rules
WORKDIR /project
COPY app app
COPY server server
RUN cd server && sh build-rules.sh
FROM node:22-bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends openjdk-17-jre-headless && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=rules /project/server/rules.jar ./rules.jar
COPY server/server.mjs server/package.json ./
RUN mkdir /app/data && chown -R node:node /app
USER node
ENV PORT=8080 ROOM_DATA=/app/data/rooms.json
EXPOSE 8080
CMD ["node","server.mjs"]
