FROM amazoncorretto:11-al2022-RC-headless

RUN ls -l /usr/lib/jvm/java-11-amazon-corretto.aarch64

#RUN yum install -y zip gzip tar
#
#WORKDIR /
#
#RUN curl -O https://download.clojure.org/install/linux-install-1.11.1.1224.sh
#RUN chmod +x ./linux-install-1.11.1.1224.sh
#RUN ./linux-install-1.11.1.1224.sh --prefix /clojure-rt
#
#COPY deps.edn deps.edn
#COPY src src
#COPY bootstrap bootstrap
#RUN chmod 755 bootstrap
#ENV GITLIBS=/gitlibs
#RUN /clojure-rt/bin/clojure
#RUN zip -r runtime.zip    \
#    bootstrap             \
#    deps.edn              \
#    /usr/lib/jvm/jre      \
#    /clojure-rt           \
#    /src                  \
#    /m2-repo              \
#    /gitlibs

