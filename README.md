# options trade tools
美股期权交易工具

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
```
开发模式:
富途RSA密钥：start/src/main/resources/key/private.key
富途交易密码md5：start/src/main/resources/key/trade.key
```

4. 初始化数据
```
owner_strategy表数初始化:
INSERT INTO `owner_strategy` (`id`, `strategy_id`, `strategy_type`, `current_stage`, `platform`, `account_id`, `code`, `market`, `owner`, `ext`) VALUES
(18,	'86c312eccaff4fe1865cf0e79432ebe3',	'wheel_strategy',	'sp',	'futu',	'123456',	'BABA',	11,	'qiyan',	'{}'),

strategy_id：建议使用uuid
strategy_type：策略类型，目前支持wheel_strategy
platform：平台 futu、longport
market：1：港股，11：美股
account_id：富途需要填写，longport不需要填，富途可以使用TrdGetAccListHelper获取账号列表
```

# 升级记录
## 2024-12-07
* init