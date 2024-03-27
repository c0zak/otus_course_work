FROM alpine:latest AS build
RUN echo http://dl-cdn.alpinelinux.org/alpine/edge/testing  >> /etc/apk/repositories
RUN apk update && \
        apk add --no-cache \
        autoconf \
        build-base \
        ca-certificates \
        cmake \
        openssl-dev \
        unbound-dev \
        yaml-dev \
        getdns-dev \
        git

RUN git clone https://github.com/getdnsapi/stubby.git
WORKDIR /stubby
RUN git checkout v0.4.3
RUN cmake .
RUN make

FROM alpine:latest
RUN echo http://dl-cdn.alpinelinux.org/alpine/edge/testing  >> /etc/apk/repositories
RUN apk update && \
    apk add --no-cache \
    ca-certificates \
    getdns \
    unbound \
    openssl \
    yaml

COPY --from=build /stubby/stubby /usr/local/bin

ENTRYPOINT ["stubby", "-C", "/config/stubby.yml"]
