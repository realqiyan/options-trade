# options trade tools
美股期权交易工具，依赖富途和长桥开放接口，目前仅支持富途交易。

# 使用说明
1. 按照富途OpenD
```
https://openapi.futunn.com/futu-api-doc/opend/opend-cmd.html

* rsa_private_key必选
在线RSA密钥对生成工具（注意保密）：https://uutool.cn/rsa-generate/ --建议本地生成
```


2. 初始化长桥开放接口权限
```
https://open.longportapp.com/docs/getting-started#%E5%BC%80%E9%80%9A%E5%BC%80%E5%8F%91%E4%B8%AD%E8%B4%A6%E6%88%B7
```

3. 初始化工程密钥
* 当前用户的home目录下创建`.options-trade`文件夹
* 创建`config.properties`文件，内容参考`example.config.properties`
* 创建`futu_rsa_private.key`文件，内容参考`example.futu_rsa_private.key`

4. 初始化数据
```
1.owner_account表数初始化（字段含义参考表结构备注）:

INSERT INTO `owner_account` (`id`, `create_time`, `owner`, `platform`, `market`, `account_id`, `otp_auth`, `status`) VALUES
(1, '2024-11-01 00:00:00', 'qiyan', 'futu', 11, '123456789', 'otpauth://totp/issuer?secret={password}&algorithm=SHA1&digits=6&period=300', 1);

初始化账号：
account_id：富途可以使用TrdGetAccListHelper获取账号列表
platform：平台 futu；
market：1：港股，11：美股；
otp_auth：otpauth认证信息，修改信息中的secret值即可；


2.owner_security表数初始化（字段含义参考表结构备注）:

INSERT INTO `owner_security` (`id`, `create_time`, `name`, `code`, `market`, `owner`, `status`) VALUES
(1, '2025-02-16 14:08:13', '阿里巴巴', 'BABA', 11, 'qiyan', 1),
(2, '2025-02-16 14:08:13', '中国海外互联网', 'KWEB', 11, 'qiyan', 1),
(3, '2025-02-16 14:08:13', '京东', 'JD', 11, 'qiyan', 1),
(4, '2025-02-16 14:08:13', '中国大盘股', 'FXI', 11, 'qiyan', 0);

3.owner_strategy表数初始化（字段含义参考表结构备注）:

INSERT INTO `owner_strategy` (`id`, `strategy_id`, `start_time`, `strategy_name`, `strategy_code`, `stage`, `status`, `code`, `lot_size`, `owner`, `ext`) VALUES
(18, '86c312eccaff4fe1865cf0e79432ebe3', '2024-11-01 00:00:00', '富途-BABA-默认策略', 'default', 'default', 1, 'BABA', 100, 'qiyan', '{}'),
(28, '024cc086a560460e90705f2ef87cac7d', '2024-11-01 00:00:00', '富途-KWEB-默认策略', 'default', 'default', 1, 'KWEB', 100, 'qiyan', '{}'),
(38, 'b3605b43f26345abbfa663abad867d38', '2024-11-01 00:00:00', '富途-JD-默认策略', 'default', 'default', 1, 'JD', 100, 'qiyan', '{}');

strategy_id：策略id，可以使用uuid工具UUIDUtils生成；
```
5. 登陆方法
账户名+totp密码登陆；

# 升级记录
## 2025-02-17 （v0.1.0）
* v0.1.0版本不兼容，升级不影响订单交易，初始化数据后从平台同步订单即可恢复交易数据；
* 支持头寸维度策略管理和追踪；
* 支持手工挂靠平台订单；
* 支持账号登陆；


## 2024-12-07
* init