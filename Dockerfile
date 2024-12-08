# FROM openjdk:8
#
# RUN mkdir -p /dingtou/config
#
# #RUN wget -O /dingtou/application.yml https://github.com/dingtoume/dingtou/blob/master/start/src/main/resources/application.yml \
# #&& wget -O /dingtou/dingtou.jar https://github.com/dingtoume/dingtou/blob/master/dingtou.jar \
# #&& echo "nohup java -jar dingtou.jar --spring.config.location=./application.yml >> application.log  2>&1 &">/dingtou/run.sh \
# #&& chmod +x /dingtou/*.sh
#
# COPY ./start/src/main/resources/application.yml /dingtou/config
# COPY ./dingtou.jar /dingtou
# COPY ./run.sh /dingtou
#
# RUN  chmod +x /dingtou/*.sh
# WORKDIR /dingtou
#
# CMD ["/dingtou/run.sh"]
