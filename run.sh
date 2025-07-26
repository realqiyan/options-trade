#!/usr/bin/env bash

# 最大等待时间(秒)
MAX_WAIT=60

# 查找并停止进程
echo "正在停止options-trade应用..."
pkill -f options-trade.jar

# 等待进程退出
WAIT_TIME=0
echo "等待进程退出，最多等待${MAX_WAIT}秒..."
while pgrep -f options-trade.jar >/dev/null; do
    if [ $WAIT_TIME -ge $MAX_WAIT ]; then
        echo "警告: 进程未在${MAX_WAIT}秒内退出，尝试强制终止..."
        pkill -9 -f options-trade.jar
        break
    fi
    
    # 每5秒输出一次等待信息
    if [ $((WAIT_TIME % 5)) -eq 0 ]; then
        echo "已等待${WAIT_TIME}秒..."
    fi
    
    sleep 1
    WAIT_TIME=$((WAIT_TIME+1))
done

# 检查是否仍有残留进程
if pgrep -f options-trade.jar >/dev/null; then
    echo "错误: 无法终止进程，请手动检查！"
    exit 1
fi

# 重新打包应用
echo "正在重新打包应用..."
./package.sh

# 启动应用
echo "正在启动应用..."
nohup java -jar options-trade.jar > nohup.out 2>&1 &
echo $! > options-trade.pid

echo "应用已启动，PID: $(cat options-trade.pid)"
