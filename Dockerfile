# Stage 1: Cache Gradle dependencies
FROM gradle:latest AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME /home/gradle/cache_home
COPY build.gradle.* gradle.properties /home/gradle/app/
WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

# Stage 2: Build Application
FROM gradle:latest AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY . /usr/src/app/
WORKDIR /usr/src/app
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Build the fat JAR, Gradle also supports shadow
# and boot JAR by default.
RUN gradle buildFatJar --no-daemon

# Stage 3: Create the Runtime Image
FROM archlinux:latest AS runtime
EXPOSE 8080:8080

# install JDK, pandoc and required latex packages
RUN pacman -Syu --noconfirm && \
    pacman -S --noconfirm jdk-openjdk pandoc texlive-basic texlive-latexextra texlive-binextra texlive-fontutils texlive-fontsrecommended texlive-langgerman base-devel git yq

# Create a non-root user, necessary for the AUR package
RUN useradd -m builduser && echo "builduser ALL=(ALL) NOPASSWD: ALL" >> /etc/sudoers

# Switch to the non-root user and build the package
USER builduser
WORKDIR /home/builduser
RUN git clone https://aur.archlinux.org/texlive-acrotex.git && \
    cd texlive-acrotex && \
    makepkg -si --noconfirm

# Clean up the build files and switch back to root
RUN rm -rf /home/builduser/latex-acrotex
USER root

# clean up
RUN pacman -Rns --noconfirm base-devel git && \
    rm -rf /var/cache/pacman/pkg/*

RUN mkdir -p /app/src/main/resources/latex
COPY --from=build /home/gradle/src/build/libs/*.jar /app/letter-api.jar
COPY --from=build /home/gradle/src/src/main/resources/latex /app/src/main/resources/latex

WORKDIR /app/
ENTRYPOINT ["java","-jar","letter-api.jar"]