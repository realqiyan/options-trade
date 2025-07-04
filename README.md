# Options Strategy Tools

卖期权策略&期权车轮策略交易工具，依赖富途和长桥开放接口，目前仅支持富途交易。

[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/realqiyan/options-trade)

## 功能特性

- 支持美股期权交易
- 多账户管理
- 策略管理和追踪
- 订单同步和管理
- TOTP 安全认证
- 头寸维度策略管理
- AI辅助交易决策

## 使用说明

### 1. 环境要求

- JDK 17 或以上
- MySQL 8.0 或以上
- Maven 3.6 或以上

### 2. 配置富途 OpenD

请参考富途官方文档：https://openapi.futunn.com/futu-api-doc/opend/opend-cmd.html

**注意事项：**

- RSA 私钥必选
- 建议使用本地工具生成 RSA 密钥对，避免在线生成可能带来的安全风险
- 需要先安装并运行富途 OpenD，确保服务正常运行

**富途 OpenD 配置步骤：**

1. 下载并安装富途 OpenD
2. 配置 OpenD 的 RSA 密钥
3. 启动 OpenD 服务
4. 验证 OpenD 服务状态

### 3. 配置长桥开放接口

请参考长桥官方文档：https://open.longportapp.com/docs/getting-started

**配置步骤：**

1. 注册长桥开发者账号
2. 创建应用并获取 API Key
3. 开通模拟环境（建议先在模拟环境测试）
4. 申请正式环境权限（如需要）

### 4. 项目配置

#### 4.1 基础配置

1. 在当前用户的 home 目录下创建 `.options-trade` 文件夹
2. 创建 `config.properties` 文件，参考 `example.config.properties`
3. 创建 `futu_rsa_private.key` 文件，参考 `example.futu_rsa_private.key`

#### 4.2 config.properties 配置说明

```properties
# 富途配置
### 富途opend的ip
futu.api.ip=127.0.0.1
### 富途opend的端口
futu.api.port=1111
### 富途交易密码的MD5值
futu.api.unlock=e10adc3949ba59abbe56e057f20f883e
```

#### 4.3 application.yml 配置说明

```yml
# 数据库配置
# 数据源配置
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/options?useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=CONVERT_TO_NULL&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=true
    username: your_username
    password: your_password
```

* 新版本需要修改`start/src/main/resources/privacy/db.properties`里的数据库配置

### 5. 数据库初始化

#### 5.1 owner_account 表初始化

```sql
INSERT INTO `owner_account` (`id`, `create_time`, `owner`, `platform`, `market`, `account_id`, `otp_auth`, `status`) VALUES
(1, '2024-11-01 00:00:00', 'qiyan', 'futu', 11, '123456789', 'otpauth://totp/issuer?secret={password}&algorithm=SHA1&digits=6&period=300', 1);
```

**说明：**

- account_id：可使用 TrdGetAccListHelper 获取账号列表
- platform：交易平台（目前支持 futu）
- market：1=港股，11=美股
- otp_auth：TOTP 认证信息，需修改 secret 值

#### 5.2 owner_security 表初始化

```sql
INSERT INTO `owner_security` (`id`, `create_time`, `name`, `code`, `market`, `owner`, `status`) VALUES
(1, '2025-02-16 14:08:13', '阿里巴巴', 'BABA', 11, 'qiyan', 1),
(2, '2025-02-16 14:08:13', '中国海外互联网', 'KWEB', 11, 'qiyan', 1),
(3, '2025-02-16 14:08:13', '京东', 'JD', 11, 'qiyan', 1),
(4, '2025-02-16 14:08:13', '中国大盘股', 'FXI', 11, 'qiyan', 0);
```

#### 5.3 owner_strategy 表初始化

```sql
INSERT INTO `owner_strategy` (`id`, `strategy_id`, `start_time`, `strategy_name`, `strategy_code`, `stage`, `status`, `code`, `lot_size`, `owner`, `ext`) VALUES
(18, '86c312eccaff4fe1865cf0e79432ebe3', '2024-11-01 00:00:00', '富途-BABA-默认策略', 'default', 'default', 1, 'BABA', 100, 'qiyan', '{}'),
(28, '024cc086a560460e90705f2ef87cac7d', '2024-11-01 00:00:00', '富途-KWEB-默认策略', 'default', 'default', 1, 'KWEB', 100, 'qiyan', '{}'),
(38, 'b3605b43f26345abbfa663abad867d38', '2024-11-01 00:00:00', '富途-JD-默认策略', 'default', 'default', 1, 'JD', 100, 'qiyan', '{}');
```

**说明：**

- strategy_id：可使用 UUIDUtils 工具生成

### 6. 项目启动

#### 6.1 编译打包

```bash
# 在项目根目录执行
mvn clean package
```

#### 6.2 启动服务

```bash
# 使用提供的启动脚本
./run.sh

# 或者直接使用 java 命令
java -jar start/target/start-0.0.1-SNAPSHOT.jar
```

#### 6.3 验证服务

1. 检查服务日志确保启动成功
2. 访问首页：`http://localhost:8888/`
3. 确认数据库连接正常
4. 验证富途 OpenD 连接状态
5. 验证长桥 API 连接状态

### 7. 使用流程

#### 7.1 账号配置

1. 使用 TrdGetAccListHelper 工具获取富途账号列表
2. 在 owner_account 表中配置账号信息
3. 设置 TOTP 认证信息

#### 7.2 证券配置

1. 在 owner_security 表中配置需要交易的证券信息
2. 确保证券代码和市场信息正确

#### 7.3 策略配置

1. 生成策略 ID（使用 UUIDUtils）
2. 在 owner_strategy 表中配置策略信息
3. 设置策略参数（如需要）

#### 7.4 登录系统

1. 准备账户名和 TOTP 密码
2. 使用登录接口进行认证
3. 保存返回的 token 用于后续接口调用

### 8. 登录方式

使用账户名 + TOTP 密码登录

## 开发说明

不跟踪本地数据库配置：

```shell
git update-index --assume-unchanged start/src/main/resources/privacy/db.properties
```

## 许可证

本项目采用 [LICENSE](LICENSE) 开源许可证
