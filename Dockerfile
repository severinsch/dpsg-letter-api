# Stage 1: Cache Gradle dependencies
FROM gradle:8.14-alpine AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY build.gradle.* gradle.properties /home/gradle/app/
WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

# Stage 2: Build Application
FROM gradle:8.14-alpine AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY . /usr/src/app/
WORKDIR /usr/src/app
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Build the fat JAR, Gradle also supports shadow
# and boot JAR by default.
RUN gradle buildFatJar --no-daemon

# Stage 3: Create the Runtime Image
FROM pandoc/latex:3.9-alpine AS runtime

EXPOSE 8080:8080

RUN apk add --no-cache openjdk25-jre yq wget unzip

# 1. Install standard LaTeX packages via tlmgr (removed ms)
RUN tlmgr option docfiles 0 && \
    tlmgr option srcfiles 0 && \
    tlmgr install \
      #tools \
      booktabs \
      koma-script \
      xkeyval \
      everypage \
      conv-xkv \
      everyshi \
      pdflscape \
      graphics \
      pgf \
      background \
      xcolor \
      geometry \
      babel \
      babel-german \
      tools \
      blindtext \
      paracol \
      lipsum \
      csquotes \
      enumitem \
      makecell \
      lastpage \
      fancyhdr \
      amsmath \
      amsfonts \
      pdfpages \
      iftex \
      l3packages \
      lm

# 2. Manually install AcroTeX from CTAN
RUN mkdir -p /tmp/acrotex && cd /tmp/acrotex && \
    wget https://mirrors.ctan.org/macros/latex/contrib/acrotex.zip && \
    unzip acrotex.zip && \
    cd acrotex && \
    tex acrotex.ins && \
    tex eforms.ins && \
    tex dljslib.ins && \
    tex insdljs.ins && \
    tex taborder.ins && \
    TEXMF_LOCAL=$(kpsewhich -var-value=TEXMFLOCAL) && \
    mkdir -p $TEXMF_LOCAL/tex/latex/acrotex && \
    cp -r * $TEXMF_LOCAL/tex/latex/acrotex/ && \
    cd /tmp/acrotex && \
    wget https://mirrors.ctan.org/macros/latex/contrib/acrotex-js.zip && \
    unzip acrotex-js.zip && \
    cd acrotex-js && \
    tex acrotex-js.ins && \
    mkdir -p $TEXMF_LOCAL/tex/latex/acrotex-js && \
    cp -r * $TEXMF_LOCAL/tex/latex/acrotex-js/ && \
    mktexlsr && \
    rm -rf /tmp/acrotex

# Define environment variables
ENV RESOURCES_BASE_PATH="/app/resources"
ENV LUA_FILTERS_BASE_PATH="/app/lua_filters"

# Set up directories
RUN mkdir -p $RESOURCES_BASE_PATH $LUA_FILTERS_BASE_PATH

# Copy artifacts from the build stage
COPY --from=build /home/gradle/src/build/libs/*.jar /app/letter-api.jar
COPY --from=build /home/gradle/src/src/main/resources/ $RESOURCES_BASE_PATH
COPY --from=build /home/gradle/src/src/main/lua_filters/ $LUA_FILTERS_BASE_PATH

WORKDIR /app/

ENTRYPOINT ["java", "-jar", "letter-api.jar"]