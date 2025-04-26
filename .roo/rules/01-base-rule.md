# 基础规则

- 保持工程结构和代码架构的连贯性
- 代码设计尽可能抽象和通用，不要出现硬编码
- 功能改动需要考虑api、provide、start模块中关联的代码，确保前后端代码都改动完整
- 在确保改动完整的情况下，每次做最小改动，不要修改和优化不相关的文件和功能

# 模块结构规则

## 模块关系

该项目采用多模块结构，遵循以下依赖关系：

- api模块：定义接口和模型，不依赖其他模块
- provider模块：依赖api模块，实现api模块定义的服务接口
- start模块：依赖api和provider模块，包含应用入口和控制器

## 编码规范

1. api模块中的接口：
   - 服务接口位于 @api/src/main/java/me/dingtou/options/service 目录
   - 每个接口方法应该有清晰的JavaDoc注释说明功能
   - 方法命名应遵循`动词+名词`的方式

2. provider模块中的实现：
   - 实现类应位于 @provider/src/main/java/me/dingtou/options/service/impl 目录
   - 实现类名应与接口名对应，并以`Impl`结尾
   - 使用`@Service`注解将服务注册到Spring容器

3. start模块中的控制器：
   - 控制器位于 @start/src/main/java/me/dingtou/options/web 目录
   - 遵循RESTful API设计原则
   - 接口路径应具有明确的语义


# API接口规范

## REST API设计

系统的Web API主要在 @start/src/main/java/me/dingtou/options/web 目录下定义：

1. 接口命名规范：
   - 使用标准HTTP方法表示操作类型（GET、POST、PUT、DELETE）
   - URL路径应使用名词而非动词
   - 使用复数形式表示资源集合
   - 使用下划线或短横线分隔单词

2. 请求参数处理：
   - GET请求参数使用URL参数传递
   - POST/PUT请求使用JSON格式传递请求体
   - 对敏感参数进行验证和转义，防止SQL注入和XSS攻击

3. 响应格式规范：
   - 统一使用JSON格式返回数据
   - 包含code、message和data三个字段
   - 成功返回时code为0，失败时为非0错误码
   - 分页查询返回totalCount、pageNo和pageSize字段

## 错误处理

1. 异常处理：
   - 使用全局异常处理器统一处理异常
   - 业务异常应转换为友好的错误信息
   - 系统异常应记录详细日志但返回通用错误信息

2. 状态码使用：
   - 200：请求成功
   - 400：请求参数错误
   - 401：未授权
   - 403：权限不足
   - 404：资源不存在
   - 500：服务器内部错误

# 数据模型规则

## 核心模型

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

## 数据持久化

1. MyBatis映射：
   - XML映射文件位于 @provider/src/main/resources/mapper 目录
   - Mapper接口位于 @provider/src/main/java/me/dingtou/options/mapper 目录

2. 数据库设计：
   - 表名应使用下划线命名法
   - 每个表应有创建时间和更新时间字段
   - 主键使用自增ID或UUID
   - 必要时添加适当的索引提高查询性能

# 前端开发规则

## 前端技术栈

该项目前端主要使用以下技术：
- Layui v2.10.1 作为UI框架
- Chart.js 用于图表展示
- Marked.js 用于Markdown渲染

## 页面结构

前端页面位于 @start/src/main/resources/static 目录，主要包括：

1. 核心页面：
   - @index.html：标的分析页面
   - @order.html：订单管理页面
   - @income.html：账户收益页面
   - @assistant.html：AI助手页面
   - @admin.html：后台管理页面
   - @sync.html：同步订单页面

2. 公共组件：
   - 页面头部和导航栏
   - 通用的弹窗和提示组件
   - 数据表格和分页组件

## 编码规范

1. JavaScript规范：
   - 使用模块化方式组织代码
   - 避免全局变量污染
   - 使用统一的错误处理和日志记录方式

2. Layui模板：
   - HTML中script标签里type="text/html"的内容是layui模板，不要当做js尝试修复
   - 模板应有清晰的注释说明用途

3. API调用：
   - 统一使用封装的AJAX方法与后端交互
   - 处理常见的错误情况和加载状态

# 任务调度规范

## 定时任务设计

系统中的任务调度相关代码主要在 @api/src/main/java/me/dingtou/options/job 目录下定义：

1. 任务设计原则：
   - 每个任务应该有单一职责
   - 任务执行应该是幂等的，避免重复执行导致数据异常
   - 关键任务应有重试机制和失败处理策略
   - 长时间运行的任务应考虑超时控制

2. 任务配置：
   - 使用Spring的@Scheduled注解配置定时任务
   - cron表达式放在配置文件中，方便调整
   - 提供手动触发任务的接口
   - 考虑集群环境下的任务重复执行问题

3. 日志记录：
   - 记录任务开始和结束时间
   - 记录关键处理步骤和数据变更
   - 异常情况详细记录错误信息
   - 可通过日志追踪任务执行全过程

## 异步处理

1. 异步任务：
   - 使用Spring的@Async注解实现异步处理
   - 配置合适的线程池参数
   - 长时间运行的异步任务需要考虑监控和中断机制

2. 消息队列：
   - 对于复杂的异步处理流程考虑使用消息队列
   - 实现生产者-消费者模式，增强系统可靠性
   - 考虑消息的持久化和重复消费问题
