FROM openjdk:13-alpine

RUN apk update
RUN apk add supervisor

RUN mkdir /kys
WORKDIR /kys

COPY ./backend/target/scala-2.13/pythia-backend.jar  pythia.jar

ADD pythia.sv.conf /etc/supervisor/conf.d/

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/pythia.sv.conf"]