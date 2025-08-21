---
trigger: always_on
alwaysApply: true
---
# 基础规则

## 编码要求

- 保持工程结构和代码架构的连贯性
- 代码设计尽可能抽象和通用，不要出现硬编码
- 功能改动需要考虑api、provide、start模块中关联的代码，确保前后端代码都改动完整
- 在确保改动完整的情况下，每次做最小改动，不要修改和优化不相关的文件和功能

## 模块结构规则

### 模块关系

该项目采用多模块结构，遵循以下依赖关系：

- api模块：定义对外的Service接口和Model，不依赖其他模块
- provider模块：依赖api模块，实现api模块定义的服务接口，内部分层：Service、Manager、DAO、Gateway
- start模块：依赖api和provider模块，包含应用入口和控制器Controller

### 架构分层

- Controller：Web控制器层，负责组合一个或多个Service完成用户请求，只能依赖 Service。
- Service：服务层，负责完成特定任务，一般依赖一个或多个Manager，只能依赖 Manager。
- Manager：业务管理层，负责具体逻辑的处理，只能依赖 DAO和Gateway。
- DAO：数据访问层，负责数据库查询和更新操作，只能依赖数据库。
- Gateway：网关层，负责与外部服务交互，只能依赖外部服务。

### 编码规范

1. api模块中的接口：
   - Model层接口位于 @api/src/main/java/me/dingtou/options/model 目录
      - 每个Model应该有清晰的JavaDoc注释说明功能
   - Service服务接口位于 @api/src/main/java/me/dingtou/options/service 目录
      - 每个接口方法应该有清晰的JavaDoc注释说明功能
      - 方法命名应遵循`动词+名词`的方式

2. provider模块中的实现：
   - Service实现类应位于 @provider/src/main/java/me/dingtou/options/service/impl 目录
      - 实现类名应与接口名对应，并以`Impl`结尾
      - 使用`@Service`注解将服务注册到Spring容器
   - Manager实现类应位于 @provider/src/main/java/me/dingtou/options/manager 目录
      - 无需定义接口，并以`Manager`结尾
      - 使用`@Component`注解将服务注册到Spring容器
   - DAO实现类应位于 @provider/src/main/java/me/dingtou/options/dao 目录
      - 需要继承`com.baomidou.mybatisplus.core.mapper.BaseMapper`，仅需要定义接口，并以`DAO`结尾，框架会自动生成实现类
   - Gateway接口和实现类应位于 @provider/src/main/java/me/dingtou/options/gateway 目录
      - 接口名应与实现类名对应，并以`Gateway`结尾
      - 使用`@Component`注解将服务注册到Spring容器
   - Job类应位于 @provider/src/main/java/me/dingtou/options/job 目录
      - 类名应与接口名对应，并以`Job`结尾
      - 使用`@Component`注解将服务注册到Spring容器

3. start模块中的控制器：
   - 控制器位于 @start/src/main/java/me/dingtou/options/web 目录
   - 控制器类名应与接口名对应，并以`Controller`结尾
   - 使用`@RestController`注解将类注册为RESTful控制器
   - 遵循RESTful API设计原则
   - 接口路径应具有明确的语义

## 数据模型规则

### 核心模型

系统定义了以下核心数据模型，位于 @api/src/main/java/me/dingtou/options/model 目录：

1. 期权相关模型：
   - Options：期权基本信息
   - Security：标的证券信息
   - OwnerOrder：订单信息
   - OwnerStrategy：用户策略

2. 模型设计规范：
   - 所有模型类应实现Serializable接口
   - 使用Lombok注解简化getter/setter
   - 每个字段都应该有注释说明用途
   - 枚举值应该使用常量类定义

### 数据持久化

1. 使用mybatis-plus框架（Mybatis-Plus 在 Mybatis 的基础上进行扩展）
   - Mapper接口位于 @provider/src/main/java/me/dingtou/options/dao 目录
   - 命名要求：XXXDAO.java

2. 数据库设计：
   - 表名应使用下划线命名法
   - 每个表应有创建时间和更新时间字段
   - 主键使用自增ID或UUID
   - 必要时添加适当的索引提高查询性能
   - 新增升级sql脚本位于 @start/src/main/resources/db/upgrade 目录，文件名格式为：{日期}_{描述}.sql

## 前端开发规则

### 前端技术栈

该项目前端使用以下技术栈：

- Layui v2.11.2 作为UI框架，所有页面都使用Layui的组件和样式。
- Chart.js 用于图表展示。
- Marked.js 用于Markdown渲染。

### 页面结构

前端页面位于 @start/src/main/resources/static 目录，主要包括：

1. 核心页面：
   - @analyze.html：标的分析页面
   - @order.html：订单管理页面
   - @summary.html：账户汇总页面
   - @assistant.html：AI助手页面
   - @admin.html：后台管理页面
   - @sync.html：同步订单页面

## 其他要求

1. 除非用户明确要求，否则无需启动应用调试，仅完成代码编写即可。
