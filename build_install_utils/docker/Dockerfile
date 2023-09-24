FROM debian:bookworm

RUN apt update -y && \
    apt install default-jdk -y && \
    apt install wget -y

ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Build binaries
RUN apt install maven -y && \
    apt install git -y && \
    git clone https://github.com/urieli/jochre.git && \
    cd /jochre/jochre_parent && \
    mvn clean package -DskipTests=true

# Set up directory structure
RUN mkdir /jochre/input && \
    mkdir /jochre/resources && \
    mkdir /jochre/output

RUN wget -O /jochre/resources/yiddish_letter_model.zip https://github.com/urieli/jochre/releases/download/v2.3.5/yiddish_letter_model.zip

RUN wget -O /jochre/resources/jochre-yiddish-lexicon-1.0.1.zip https://github.com/urieli/jochre/releases/download/v2.3.5/jochre-yiddish-lexicon-1.0.1.zip

ENTRYPOINT ["java", "-jar" , "-Xmx3G"]
