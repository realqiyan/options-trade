# 系统项目结构

## 整体架构

这是一个基于Spring Boot 3.3.9的期权交易系统，采用了分层架构：

1. api模块：包含业务逻辑、模型定义和服务接口
2. provider模块：实现API模块定义的服务接口
3. start模块：包含Spring Boot应用的启动类、控制器和前端资源

## 主要技术栈

### 后端

- Java 17
- Spring Boot 3.3.9
- MyBatis (用于数据库访问)
- MySQL数据库

### 前端

- Layui v2.10.1 (UI框架)
- Chart.js (图表库)
- Marked.js (Markdown渲染)

## 项目结构详解

### 1. API模块

```text
api/
└── src/main/java/me/dingtou/options/
    ├── model/            # 数据模型定义
    ├── constant/         # 常量定义
    ├── service/          # 服务接口
    ├── job/              # 任务调度
    └── util/             # 工具类
```

#### 核心模型类包括

- OwnerOrder：订单模型
- Options：期权模型
- Security：证券模型
- OwnerStrategy：用户策略模型

### 2. Start模块

```text
start/
└── src/main/
    ├── java/me/dingtou/options/
    │   ├── web/              # Web控制器
    │   │   ├── model/        # Web层模型
    │   │   ├── filter/       # 过滤器
    │   │   └── util/         # Web工具类
    │   ├── config/           # 配置类
    │   └── Application.java  # 启动类
    └── resources/
        ├── static/           # 静态资源
        │   ├── layui/        # UI框架
        │   ├── *.html        # HTML页面
        │   └── *.js          # JavaScript文件
        ├── db/               # 数据库相关
        └── application.yml   # 应用配置
```

#### 主要Web控制器

- WebApiController：核心业务API
- WebAIController：AI聊天服务
- WebAdminController：管理功能

### 3. 前端页面

- index.html：标的分析页面
- order.html：订单管理页面
- income.html：账户收益页面
- assistant.html：AI助手页面
- admin.html：后台管理页面
- sync.html：同步订单页面
