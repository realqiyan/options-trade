#!/usr/bin/env bash

# 检查是否已经有进程在运行
PID=$(pgrep -f options-trade.jar)

if [ -n "$PID" ]; then
    echo "进程已存在，PID: $PID，正在关闭..."
    kill $PID
    sleep 3  # 等待进程关闭
fi

# 重新打包应用
./package.sh

# 启动应用并记录PID
nohup java -jar options-trade.jar > nohup.out 2>&1 &
echo $! > options-trade.pid
echo "应用已启动，PID: $!"
