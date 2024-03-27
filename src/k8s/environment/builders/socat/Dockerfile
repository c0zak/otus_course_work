FROM alpine:latest
RUN apk add --no-cache socat
COPY ./initial.sh /starter/initial.sh

ENTRYPOINT [ "/bin/sh", "/starter/initial.sh"]
