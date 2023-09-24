FROM debian:bookworm

RUN apt update -y && \
    apt install default-jdk -y

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

RUN apt install maven -y && \
    apt install git -y && \
    git clone https://github.com/urieli/jochre.git && \
    cd jochre/jochre_parent && \
    mvn clean package -DskipTests=true

