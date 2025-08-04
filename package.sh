#!/usr/bin/env bash
export LANG=zh_CN.UTF-8
mvn clean package -Dmaven.test.skip=true -Dfile.encoding=UTF-8
cp start/target/options-trade.jar .
