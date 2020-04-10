FROM openjdk:13-alpine

RUN apk update
RUN apk add supervisor

RUN mkdir /kys
WORKDIR /kys

COPY ./backend/target/scala-2.13/ScheduleChecker-backend.jar  ScheduleChecker.jar

ADD schedulechecker.sv.conf /etc/supervisor/conf.d/

CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/schedulechecker.sv.conf"]