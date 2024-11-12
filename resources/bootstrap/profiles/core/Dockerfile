# base =========================================================================
FROM debian:bookworm-slim AS base

# The AWS SDK library makes some illegal reflections code access. According
# to the devs it won't be fixed because they are working in the v2 of the
# SDK [1]. Previous JVM version were just generating a warning, but since we
# updated to JDK 17 the JVM blocks those illegal accesses by default. That
# generates unexpected behaviour and exceptions in the Amazonica library.
# By using this flag we tell the JVM not to block them.
#
# As far as we know the flag doesn't have any security implications. The access
# is blocked by default because mantainability reasons[2]. The libraries shouldn't
# be calling to low-level Java code because is not part of the public API and
# is subject to change.
#
# Note that this is just for production use. For development we added the same
# flag to the project.clj.
# [1] https://github.com/mcohen01/amazonica/issues/323#issuecomment-461392822
# [2] http://openjdk.java.net/jeps/261
# [3] https://github.com/aws/aws-sdk-java/issues/2288#issuecomment-856995652
ENV JVM_OPTS="-Djava.awt.headless=true --add-opens java.base/java.lang=ALL-UNNAMED"
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"

RUN set -ex; \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    libsodium23=1.0.18-1 \
    libfreetype6=2.12.1+dfsg-5+deb12u3 \
    fontconfig=2.14.1-4 \
    poppler-utils=22.12.0-2+b1 \
    imagemagick=8:6.9.11.60+dfsg-1.6+deb12u2 && \
    apt-get -y autoremove && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    mkdir --parents "${JAVA_HOME}/lib" && \
    printf 'version=1\nsequence.allfonts=default\n' > "${JAVA_HOME}/lib/fontconfig.properties" && \
    sed -i -e 's|<policy domain="coder" rights="none" pattern="PDF" />|<policy domain="coder" rights="write" pattern="PDF" />|g' /etc/ImageMagick-6/policy.xml

# dev ==========================================================================
FROM base AS dev

# hadolint ignore=DL3022
COPY --from=eclipse-temurin:21-jdk $JAVA_HOME $JAVA_HOME

ENV LEIN_VERSION=2.11.2
ENV CLJ_KONDO_VERSION=2024.08.29
ENV BINARIES_INSTALL_PATH=/usr/local/bin

RUN set -eux; \
    apt-get update && \
    apt-get install -y --no-install-recommends \
    ca-certificates=20230311 \
    runit=2.1.2-54 \
    curl=7.88.1-10+deb12u8 \
    unzip=6.0-28 && \
    useradd --home-dir /home/hop --create-home --shell /bin/bash --user-group hop && \
    curl -sL -o "${BINARIES_INSTALL_PATH}/lein" "https://codeberg.org/leiningen/leiningen/raw/tag/${LEIN_VERSION}/bin/lein-pkg" && \
    chmod 755 "${BINARIES_INSTALL_PATH}/lein" && \
    mkdir -p /usr/share/java && \
    curl -sL -o "/usr/share/java/leiningen-$LEIN_VERSION-standalone.jar" "https://codeberg.org/leiningen/leiningen/releases/download/$LEIN_VERSION/leiningen-$LEIN_VERSION-standalone.jar" && \
    curl -sL -o /tmp/install-clj-kondo "https://raw.githubusercontent.com/clj-kondo/clj-kondo/master/script/install-clj-kondo" && \
    chmod 755 /tmp/install-clj-kondo && \
    ./tmp/install-clj-kondo --version "${CLJ_KONDO_VERSION}" --dir "${BINARIES_INSTALL_PATH}" && \
    apt-get -y purge curl unzip && \
    apt-get -y autoremove && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

COPY docker/run-as-user.sh "${BINARIES_INSTALL_PATH}"

WORKDIR /app

ENTRYPOINT ["run-as-user.sh"]

# lint-and-test  ===============================================================
FROM dev AS ci

COPY app/project.clj /app/
RUN lein deps
COPY app /app

# build-uberjar=================================================================
FROM ci AS build-uberjar

RUN lein uberjar

# prod =========================================================================
FROM base AS prod

ARG GIT_TAG
ENV GIT_TAG=$GIT_TAG

WORKDIR /opt

# hadolint ignore=DL3022
COPY --from=eclipse-temurin:21-jre $JAVA_HOME $JAVA_HOME

COPY --from=build-uberjar /app/target/{{project.name}}-standalone.jar {{project.name}}-standalone.jar

# hadolint ignore=DL3025
CMD java -Djava.awt.headless=true \
    -XshowSettings:vm \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=70.0 \
    -server \
    -jar {{project.name}}-standalone.jar :duct/migrator && \
    java -Djava.awt.headless=true \
    -XshowSettings:vm \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=70.0 \
    -server \
    -jar {{project.name}}-standalone.jar :duct/daemon

EXPOSE 3000
