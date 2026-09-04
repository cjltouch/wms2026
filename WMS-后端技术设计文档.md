# WMS 仓库管理系统 - 后端技术设计文档（精简单体版）

> **文档版本**: v1.0  
> **编制日期**: 2026-08-24  
> **架构风格**: Spring Boot 单体应用（直连 MySQL，无 Redis / RocketMQ 等中间件，适配小型项目快速落地）  
> **对标产品需求文档**: [WMS-仓库管理系统-产品需求文档.md](./WMS-仓库管理系统-产品需求文档.md)  
> **对标数据库脚本**: [WMS-SQL-核心业务表结构.sql](./WMS-SQL-核心业务表结构.sql)

---

## 目录
- [1. 设计原则与选型](#1-设计原则与选型)
- [2. 技术栈清单（精简版）](#2-技术栈清单精简版)
- [3. 系统分层架构](#3-系统分层架构)
- [4. 标准项目目录结构](#4-标准项目目录结构)
- [5. pom.xml 核心依赖](#5-pomxml-核心依赖)
- [6. application.yml 核心配置](#6-applicationyml-核心配置)
- [7. 数据库访问层（MyBatis-Plus + MySQL 直连）](#7-数据库访问层mybatis-plus--mysql-直连)
- [8. 权限体系（JWT + 本地 ThreadLocal 会话）](#8-权限体系jwt--本地-threadlocal-会话)
- [9. 核心模块设计（13 个业务模块）](#9-核心模块设计13-个业务模块)
- [10. 批量操作单据的 DTO 结构规范](#10-批量操作单据的-dto-结构规范)
- [11. 事务边界与并发控制](#11-事务边界与并发控制)
- [12. 全局异常与统一响应](#12-全局异常与统一响应)
- [13. 审计日志 & 操作日志](#13-审计日志--操作日志)
- [14. 库存核心业务逻辑（入库/出库/调拨/报损/盘点）](#14-库存核心业务逻辑入库出库调拨报损盘点)
- [15. 报表 & 导出（EasyExcel）](#15-报表--导出easyexcel)
- [16. 单元测试 & 部署](#16-单元测试--部署)
- [17. 未来升级为微服务的路径（可选）](#17-未来升级为微服务的路径可选)

---

## 1. 设计原则与选型
| 原则 | 说明 |
|-----|------|
| **轻量化** | 移除 Redis / RocketMQ / Nacos / Sentinel / Seata / XXL-JOB，减少部署与运维复杂度 |
| **MySQL 直连** | 所有热点数据、库存计算、分布式锁均直接在 MySQL 层完成（`SELECT ... FOR UPDATE`、乐观锁 `version`） |
| **可升级** | 目录分层与模块保持与微服务版本一致，未来升级 Spring Cloud Alibaba 仅需加依赖 + 拆分 Jar，业务代码零改动 |
| **高可追溯** | 所有单据状态流转写 `_status_log` 表，所有库存变动写 `wms_inventory_log`，满足审计要求 |
| **小步快跑** | 先完整跑通 13 模块 CRUD + 批量单据 + 库存三量联动，再逐步叠加报表、工作流、预警定时任务 |

---

## 2. 技术栈清单（精简版）
| 层次 | 组件 | 版本（建议） | 作用 |
|-----|------|-------------|------|
| JDK | OpenJDK / Oracle JDK | 17 LTS | 编译与运行时，`records`、`sealed class`、虚拟线程可按需启用 |
| 基础框架 | Spring Boot | 3.3.x（最新稳定 3.x） | 单体容器、自动装配、AOP、事务、定时任务 `@Scheduled` |
| ORM | MyBatis-Plus | 3.5.7+ | 代码生成、`IService/BaseMapper`、分页、逻辑删除、自动填充、Lambda 条件构造 |
| 代码生成 | MyBatis-Plus Generator / local DIY | - | 根据 SQL 一键生成 Entity / Mapper / Service / Controller |
| 数据库 | MySQL | 9.x（或 8.0.36+） | 主库，直连 HikariCP 连接池；`utf8mb4_0900_ai_ci` 排序 |
| 连接池 | HikariCP | Spring Boot 内置 | 数据库连接池，默认即可 |
| 鉴权 | Java JWT (auth0) | 4.4.0 | JWT Token 签发与校验（访问 Token + 刷新 Token 双令牌） |
| 密码加密 | Spring Security BCrypt | Spring Boot 内置 | 密码加盐哈希，强度 10 |
| 参数校验 | Jakarta Validation | Spring Boot starter-validation 内置 | `@NotBlank / @Size / @Min / @Valid` 分组校验 |
| JSON | Jackson | Spring Boot 内置 | Long → String（防 JS 精度丢失）、日期统一 `yyyy-MM-dd HH:mm:ss` |
| Excel | EasyExcel | 3.3.x | 百万行级导入导出，不 OOM |
| 工具类 | Hutool | 5.8.x | 日期/集合/字符串/BeanUtil/树构建工具集 |
| Knife4j（OpenAPI 3） | Knife4j openapi3 | 4.5.0 | Swagger 界面 + 接口自动文档（替代手动写 API 文档） |
| 日志 | Logback + SLF4J | Spring Boot 内置 | 按天滚动日志，INFO/WARN/ERROR 分文件 |
| 分页 | MyBatis-Plus PaginationInnerInterceptor | - | 物理分页 + 数据权限插件 |
| 数据权限 | 自定义 MyBatis Interceptor（AOP 拼接 warehouse_id/dept_id） | - | 多仓/多部门按权限自动过滤数据 |
| 构建工具 | Maven | 3.9+ | 打包 Jar 部署 |
| 应用服务器 | 内嵌 Tomcat 10.1 | Spring Boot 内置 | 无需外部 Tomcat，`java -jar` 启动 |

> **明确不引入（本项目规模不需要）**：Redis、RocketMQ/Kafka、Nacos、Sentinel、Seata、ShardingSphere、XXL-JOB、ElasticSearch、MinIO/OSS、Flowable、Docker/K8s。  
> 如未来需要，按本目录分层平滑接入。

---

## 3. 系统分层架构

```
┌────────────────────────────────────────────────────────────────────┐
│                         前端（Vue3 + Element Plus）                  │
└────────────────────────────────┬───────────────────────────────────┘
                                 │ HTTP / HTTPS
┌────────────────────────────────▼───────────────────────────────────┐
│  Controller 控制层                                                │
│  @RestController + @RequestMapping + @Valid + @PreAuthorize        │
│  统一响应 R<T> 包装、参数校验、权限注解                              │
└────────────────────────────────┬───────────────────────────────────┘
                                 │
┌────────────────────────────────▼───────────────────────────────────┐
│  AOP 切面层                                                        │
│  ├─ GlobalExceptionHandler        全局异常（业务/参数/数据库）      │
│  ├─ DataPermissionInterceptor    数据权限自动过滤（仓库/部门）       │
│  ├─ OperationLogAspect           操作日志注解（sys_oper_log）       │
│  └─ RepeatSubmitAspect           防重提交（同单据短时重复提交拦截）  │
└────────────────────────────────┬───────────────────────────────────┘
                                 │
┌────────────────────────────────▼───────────────────────────────────┐
│  Service 业务层                                                    │
│  ├─ 主表 Service（PurchaseService / StockInService …）              │
│  │    ├─ DTO 装配（MapStruct / BeanUtil）                          │
│  │    ├─ @Transactional(rollbackFor = Exception.class)             │
│  │    ├─ 库存变更统一走 InventoryChangeHandler（策略模式）          │
│  │    └─ 状态流转统一走 StatusFlowHandler（状态机）                 │
│  └─ Manager 聚合层（跨 Service 调用，如采购到货→入库回写）          │
└────────────────────────────────┬───────────────────────────────────┘
                                 │
┌────────────────────────────────▼───────────────────────────────────┐
│  Mapper 数据访问层（MyBatis-Plus + 手写 XML）                      │
│  ├─ BaseMapper<T> 单表 CRUD + LambdaWrapper                        │
│  ├─ IPage<T> 分页查询（复杂多表 JOIN 在 XML 写 SQL）                │
│  └─ 复杂聚合 SQL（报表 / 看板）写 XML ResultMap                     │
└────────────────────────────────┬───────────────────────────────────┘
                                 │
┌────────────────────────────────▼───────────────────────────────────┐
│                    MySQL（HikariCP 连接池，直连）                   │
│  wms_db 31 对象：21 张 wms_* 业务表 + 8 张 sys_* 系统表 + 2 视图   │
└────────────────────────────────────────────────────────────────────┘
```

### 3.1 模块内三层代码职责分界线（严格遵守）
| 层 | 允许内容 | 禁止内容 |
|----|---------|---------|
| **Controller** | 参数接收、@Valid 校验、调用一个 Service 方法、R 包装 | 禁止出现事务、禁止写业务 if/else、禁止直接调 Mapper |
| **Service** | 一个完整业务动作（含多步骤）、@Transactional、业务校验、状态机、库存计算 | 禁止接收 HttpServletRequest、禁止写 JSON 序列化逻辑、禁止组装返回前端的 VO（由 Controller 层转） |
| **Mapper** | SQL 查询（#{} 参数化，不得用 ${} 拼 SQL）、分页、聚合 | 禁止出现业务判断、禁止写日志 |

---

## 4. 标准项目目录结构
```
wms-backend/                                 Spring Boot 根 Maven 项目
├── pom.xml
└── src
    ├── main
    │   ├── java/com/company/wms/
    │   │   ├── WmsApplication.java          @SpringBootApplication + @EnableScheduling
    │   │   │
    │   │   ├── common/                      公共组件（跨模块复用，不得依赖 business）
    │   │   │   ├── R.java                     统一响应体<T>
    │   │   │   ├── ResultCode.java            错误码枚举（401/403/404/500/业务码 10xxx）
    │   │   │   ├── PageReq.java               分页请求 DTO（pageNum/pageSize/orderBy）
    │   │   │   ├── PageRsp.java               分页响应 DTO（total/rows）
    │   │   │   ├── BaseEntity.java            @TableId + createBy/createTime/updateBy/updateTime + version
    │   │   │   ├── BatchStatusReq.java        批量审核/作废/反审核请求（ids[] + remark）
    │   │   │   ├── exception/                 BizException、ParamException、AuthException
    │   │   │   ├── handler/                   GlobalExceptionHandler
    │   │   │   ├── validator/                 自定义校验注解（@EnumValue、@Mobile 等）
    │   │   │   └── utils/                     JSON、Date、Tree、SnowflakeId、BigDecimalCompareUtil
    │   │   │
    │   │   ├── config/                      Spring 配置类
    │   │   │   ├── MybatisPlusConfig.java     PaginationInnerInterceptor、数据权限拦截器、自动填充 MetaObjectHandler
    │   │   │   ├── WebMvcConfig.java          CORS、静态资源、拦截器
    │   │   │   ├── Knife4jConfig.java         OpenAPI 3 + 分组
    │   │   │   ├── SecurityConfig.java        BCryptPasswordEncoder Bean、过滤器放行白名单
    │   │   │   ├── JwtConfig.java             密钥、过期时间读取
    │   │   │   └── EasyExcelConfig.java       全局样式
    │   │   │
    │   │   ├── auth/                        登录 & JWT 鉴权
    │   │   │   ├── JwtTokenProvider.java       JWT 签发/校验/刷新
    │   │   │   ├── AuthContextHolder.java      ThreadLocal<LoginUser>（userId、username、warehouseIds、roleCodes、dataScope）
    │   │   │   ├── JwtAuthenticationFilter.java  OncePerRequestFilter，Header→Token→校验→注入上下文
    │   │   │   ├── LoginService.java           登录/刷新 Token/登出
    │   │   │   └── PreAuthorizeAspect.java     @PreAuthorize("hasAuthority('sys:user:add')") 接口级权限
    │   │   │
    │   │   ├── system/                      系统模块（sys_* 表）
    │   │   │   ├── controller/                 SysUserController/SysRoleController/SysMenuController/SysDeptController/SysLogController
    │   │   │   ├── service/                    UserService/RoleService/MenuService/DeptService/OperLogService/LoginLogService
    │   │   │   ├── mapper/                     UserMapper/RoleMapper/MenuMapper/DeptMapper/OperLogMapper/LoginLogMapper
    │   │   │   ├── entity/                     SysUser/SysRole/SysMenu/SysDept/SysUserRole/SysRoleMenu/SysOperLog/SysLoginLog
    │   │   │   ├── dto/req/                    UserPageReq/UserSaveReq/RoleSaveReq/MenuSaveReq
    │   │   │   └── dto/rsp/                    LoginRsp/UserInfoRsp/RouterVO（菜单树）
    │   │   │
    │   │   ├── business/                    WMS 业务模块（wms_* 表）
    │   │   │   ├── basedata/                   基础资料（商品/供应商/仓库）
    │   │   │   │   ├── controller/               GoodsController/SupplierController/CustomerController
    │   │   │   │   ├── controller/               WarehouseController/AreaController/LocationController
    │   │   │   │   ├── controller/               CategoryController/BrandController/UnitController
    │   │   │   │   ├── service/                  GoodsService/SupplierService/...
    │   │   │   │   ├── mapper/
    │   │   │   │   ├── entity/
    │   │   │   │   └── dto/                      goods/ supplier/ warehouse/ 下各自 saveReq/pageReq/rsp
    │   │   │   │
    │   │   │   ├── purchase/                   采购管理
    │   │   │   │   ├── controller/PurchaseOrderController.java
    │   │   │   │   ├── service/
    │   │   │   │   │   ├── PurchaseOrderService.java      主表 CRUD + 提交/审核/反审核/作废
    │   │   │   │   │   └── PurchaseStatusLogService.java  状态流转日志（每次状态变更写一条）
    │   │   │   │   ├── mapper/PurchaseOrderMapper.java  / PurchaseOrderItemMapper.java  / PurchaseStatusLogMapper.java
    │   │   │   │   ├── entity/  PurchaseOrder.java / PurchaseOrderItem.java / PurchaseStatusLog.java
    │   │   │   │   └── dto/
    │   │   │   │       ├── req/PurchaseSaveReq.java          主表 + List<Item> 保存（批量商品）
    │   │   │   │       ├── req/PurchasePageReq.java          分页查询（单号/供应商/日期/状态）
    │   │   │   │       ├── req/PurchaseAuditReq.java         审核/反审核/作废（id + remark）
    │   │   │   │       └── rsp/PurchaseDetailRsp.java        主表 + List<Item> + 状态流转时间线
    │   │   │   │
    │   │   │   ├── stockin/                    入库管理
    │   │   │   ├── stockout/                   出库管理
    │   │   │   ├── transfer/                   调拨管理
    │   │   │   ├── sale/                       销售管理
    │   │   │   ├── loss/                       报损管理
    │   │   │   ├── check/                      盘点管理
    │   │   │   └── inventory/                  库存核心
    │   │   │       ├── InventoryService.java             库存快照查询 + 可用/锁定/实存三量视图
    │   │   │       ├── InventoryLogService.java          库存流水
    │   │   │       ├── handler/InventoryChangeHandler.java  策略接口
    │   │   │       ├── handler/impl/
    │   │   │       │     ├── StockInInventoryHandler.java        入库：+实存 +可用
    │   │   │       │     ├── StockOutInventoryHandler.java       出库：-实存 -锁定 或 -实存 -可用
    │   │   │       │     ├── StockOutLockInventoryHandler.java   出库预扣：+锁定 -可用（发运后释放锁定并减实存）
    │   │   │       │     ├── TransferInventoryHandler.java       调拨：出库仓-实存 + 入库仓+实存
    │   │   │       │     ├── LossInventoryHandler.java           报损：-实存 +报损金额
    │   │   │       │     ├── CheckInventoryHandler.java          盘点：盈亏调整
    │   │   │       │     └── PurchaseReturnInventoryHandler.java 采购退货：-实存 -可用
    │   │   │       └── dto/                                       库存查询、流水、预警、看板
    │   │   │
    │   │   ├── report/                      报表 / 统计 / 看板
    │   │   │   ├── DashboardController.java    仓储统计首页看板（入/出/存金额 Top10 SKU）
    │   │   │   ├── ReportController.java       导出报表（采购对账单、出入库明细、库存余额、效期预警）
    │   │   │   ├── manager/ReportManager.java  复杂聚合查询（跨 3+ 表 JOIN、Excel 生成、返回流）
    │   │   │   └── mapper/ReportMapper.java    复杂 SQL 写 XML
    │   │   │
    │   │   └── job/                         定时任务（Spring @Scheduled，单例即可，不用 XXL-JOB）
    │   │       ├── ExpireWarningJob.java       每日 08:00 扫描效期 30 天内批次，写入站内消息/导出报表
    │   │       ├── InventoryRecalcJob.java     每日 03:00 凌晨重建库存汇总冗余字段（兜底对账）
    │   │       └── OperationLogCleanJob.java   每月 1 号清理 180 天前的操作日志
    │   │
    │   └── resources/
    │       ├── application.yml                  主配置
    │       ├── application-dev.yml              开发环境
    │       ├── application-prod.yml             生产环境
    │       ├── mapper/                          MyBatis XML（按模块分子目录 purchase/ stockin/ report/）
    │       ├── sql/                             升级脚本（V1__init.sql、V1.1__add_idx.sql…）
    │       └── templates/                       导出 Excel 模板（采购单模板、库存盘点模板）
    └── test/java/com/company/wms/
        ├── InventoryServiceTest.java            库存三量联动单元测试（入库/出库/调拨/报损 断言）
        ├── PurchaseFlowTest.java                采购→到货→入库→对账流程集成测试
        └── BaseTest.java                        @SpringBootTest + 测试 H2/独立 test 库
```

---

## 5. pom.xml 核心依赖
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.3.3</version> <!-- 锁定最新 3.3.x -->
    </parent>
    <groupId>com.company</groupId>
    <artifactId>wms-backend</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
        <mybatis-plus.version>3.5.7</mybatis-plus.version>
        <knife4j.version>4.5.0</knife4j.version>
        <easyexcel.version>3.3.4</easyexcel.version>
        <hutool.version>5.8.32</hutool.version>
        <jwt.version>4.4.0</jwt.version>
    </properties>

    <dependencies>
        <!-- ========== Spring Boot 核心 ========== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-aop</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
            <!-- 仅用 BCryptPasswordEncoder，默认的 UsernamePasswordAuthenticationFilter 禁用，走自定义 JWT Filter -->
        </dependency>

        <!-- ========== 数据库：MySQL 直连 ========== -->
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- ========== JWT ========== -->
        <dependency>
            <groupId>com.auth0</groupId>
            <artifactId>java-jwt</artifactId>
            <version>${jwt.version}</version>
        </dependency>

        <!-- ========== API 文档（Knife4j = OpenAPI 3 UI）========== -->
        <dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
            <version>${knife4j.version}</version>
        </dependency>

        <!-- ========== 工具类 ========== -->
        <dependency>
            <groupId>cn.hutool</groupId>
            <artifactId>hutool-all</artifactId>
            <version>${hutool.version}</version>
        </dependency>
        <dependency>
            <groupId>com.alibaba</groupId>
            <artifactId>easyexcel</artifactId>
            <version>${easyexcel.version}</version>
        </dependency>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- ========== 测试 ========== -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <finalName>wms-backend</finalName>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId></exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 6. application.yml 核心配置
```yaml
server:
  port: 8080
  servlet:
    context-path: /wms-api
  tomcat:
    uri-encoding: UTF-8
    max-http-form-post-size: 10MB

spring:
  application:
    name: wms-backend
  profiles:
    active: dev
  jackson:
    date-format: yyyy-MM-dd HH:mm:ss
    time-zone: GMT+8
    default-property-inclusion: non_null
    serialization:
      write-dates-as-timestamps: false
      WRITE_BIGDECIMAL_AS_PLAIN: true
    deserialization:
      FAIL_ON_UNKNOWN_PROPERTIES: false

  # ==================== MySQL 直连（唯一数据源） ====================
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://127.0.0.1:3306/wms_db?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&nullCatalogMeansCurrent=true
    username: root
    password: ""      # 本项目小，本地 root 空密码；生产环境请用独立账号 + 环境变量
    type: com.zaxxer.hikari.HikariDataSource
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 600000
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
      pool-name: WMS-HikariCP

  # 上传文件
  servlet:
    multipart:
      enabled: true
      max-file-size: 20MB
      max-request-size: 50MB

  # JWT（密钥建议从环境变量注入，写死此处仅开发用）
  jwt:
    secret: "change-me-in-prod-please-use-64bytes-strong-key-XXXXXXXXXXXXXX1234567890"
    access-token-expire-minutes: 720     # 12 小时
    refresh-token-expire-days: 7         # 刷新 Token 7 天

# ==================== MyBatis-Plus ====================
mybatis-plus:
  mapper-locations: classpath*:mapper/**/*.xml
  type-aliases-package: com.company.wms.**.entity
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl   # 开发打印 SQL；生产改为 Slf4jImpl
  global-config:
    banner: false
    db-config:
      id-type: ASSIGN_ID          # 雪花算法 64bit Long（避免 JS 精度丢失，Jackson 统一序列化为 String）
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
      update-strategy: not_null
      insert-strategy: not_null

# ==================== Knife4j / OpenAPI ====================
knife4j:
  enable: true
  setting:
    language: zh_cn
springdoc:
  swagger-ui:
    path: /swagger-ui.html
    tags-sorter: alpha
  api-docs:
    path: /v3/api-docs
  group-configs:
    - group: '系统模块'
      paths-to-match: /api/system/**
      packages-to-scan: com.company.wms.system
    - group: 'WMS 业务模块'
      paths-to-match: /api/wms/**
      packages-to-scan: com.company.wms.business
    - group: '报表统计'
      paths-to-match: /api/report/**
      packages-to-scan: com.company.wms.report

# ==================== 日志 ====================
logging:
  level:
    root: INFO
    com.company.wms: DEBUG
    com.company.wms.mapper: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/wms.log
    total-size-cap: 5GB
    max-history: 30
  logback:
    rollingpolicy:
      max-file-size: 50MB
```

---

## 7. 数据库访问层（MyBatis-Plus + MySQL 直连）
### 7.1 自动填充（MetaObjectHandler）
```java
@Component
public class WmsMetaObjectHandler implements MetaObjectHandler {
    @Override public void insertFill(MetaObject meta) {
        Long uid = AuthContextHolder.getUserId();
        this.strictInsertFill(meta, "createBy", Long.class, uid);
        this.strictInsertFill(meta, "updateBy", Long.class, uid);
        this.strictInsertFill(meta, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(meta, "updateTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(meta, "version", Integer.class, 0);
        this.strictInsertFill(meta, "deleted", Integer.class, 0);
    }
    @Override public void updateFill(MetaObject meta) {
        Long uid = AuthContextHolder.getUserId();
        this.strictUpdateFill(meta, "updateBy", Long.class, uid);
        this.strictUpdateFill(meta, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}
```

### 7.2 BaseEntity 继承
```java
@Data
public abstract class BaseEntity {
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    @TableField(fill = FieldFill.INSERT)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long createBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long updateBy;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
    @Version
    @TableField(fill = FieldFill.INSERT)
    private Integer version;
    @TableLogic
    @JsonIgnore
    private Integer deleted;
}
```

### 7.3 分页拦截器 + 防全表更新删除
```java
@Configuration
public class MybatisPlusConfig {
    @Bean public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor it = new MybatisPlusInterceptor();
        // 分页
        it.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 防全表更新/删除（生产必须）
        it.addInnerInterceptor(new BlockAttackInnerInterceptor());
        // 数据权限（自动拼接 warehouse_id / dept_id）
        it.addInnerInterceptor(new DataPermissionLineInnerInterceptor());
        return it;
    }
}
```

### 7.4 数据权限拦截（多仓/多部门）
- 5 种数据范围：`ALL(1)/CUSTOM(2)/DEPT(3)/DEPT_AND_CHILD(4)/SELF(5)`（跟 sys_role.data_scope 字段对齐）
- 对 `wms_*` 业务表统一追加 `warehouse_id IN (...)` / `create_by = ?`，**无需在每个 Controller/Service 手动写**

---

## 8. 权限体系（JWT + 本地 ThreadLocal 会话）
### 8.1 双令牌机制（无 Redis，Token 自包含，服务端无状态）
| 令牌 | 生命周期 | 载荷 | 用途 |
|-----|---------|------|------|
| `access_token` | 720 分钟（12h） | userId / username / roleCodes / warehouseIds / dataScope | 每次请求 Header `Authorization: Bearer <token>` |
| `refresh_token` | 7 天 | userId + `rt:<uuid>` 标识 | `/api/system/auth/refresh-token` 换新 access_token |

> 注：由于 **不引入 Redis**，无法主动吊销 Token，以下 2 点补足：
> - Token 过期时间短（12h）；
> - 密码修改、账号封禁、退出登录，由 `user.password_update_time > token.iat` 判定失效（登录时在 DB 更新 `pwdUpdateAt`，JWT 中放入 `iat` 签发时间，校验时比对）。

### 8.2 接口级权限模型
- 菜单 `sys_menu.perms` 字段形如 `wms:purchase:add,wms:purchase:audit`；
- Controller 方法上写 `@PreAuthorize("hasAuthority('wms:purchase:add')")`；
- `PreAuthorizeAspect` 从 AuthContextHolder 取出用户权限 set 匹配，不匹配抛 403。

### 8.3 菜单/路由树
- 登录成功后返回 `routers: []`（菜单树 + 按钮级权限 `perms`），前端动态注册路由与按钮权限隐藏。

---

## 9. 核心模块设计（13 个业务模块）
| 模块 | 实体表（3 张/模块） | 关键能力 |
|-----|--------------------|---------|
| 采购 | `wms_purchase_order` / `_item` / `_status_log` | 批量商品明细、提交→审核→到货（回写 `delivered_qty/unreceived_qty`）→ 对账视图 `v_purchase_reconcile` |
| 入库 | `wms_stock_in` / `_item` / `_status_log` | 关联源单（采购单/调拨单/生产入库），批次号、生产日期、效期、SN 序列号 JSON、库位分配；审核触发入库库存 Handler |
| 出库 | `wms_stock_out` / `_item` / `_status_log` | 关联源单（销售单/调拨出库/领料）；三量：`expected_qty/actual_qty/picked_qty`；双单价：`cost_price/sale_price`；双金额：`subtotal_cost/subtotal_sale`；批次分配 JSON：FIFO / FEFO / 手动 |
| 调拨 | `wms_transfer_order` / `_item` / `_status_log` | 出库仓 + 入库仓；先审核→出库仓扣减→入库仓上架入库；两仓同步事务 |
| 销售 | `wms_sale_order` / `_item` / `_status_log` | 客户 + 折扣；销售价/销售小计；销售审核触发出库预扣 `locked_qty` |
| 报损 | `wms_stock_loss` / `_item` / `_status_log` | 原因分级；审核扣实存 + 写报损金额进库存账 |
| 盘点 | `wms_check_order` / `_item` / `_status_log` | 盘点任务按仓库/库位/ABC 分类生成；账面 vs 实盘；盘盈盘亏走 CheckInventoryHandler |
| 库存核心 | `wms_inventory` / `wms_inventory_log` | 四要素唯一索引 `uk_wh_sku_loc_batch`；库存实存/锁定/可用；每次单据审核写一条流水 |
| 商品基础 | `wms_goods_spu` / `wms_goods_sku` + `category/brand/unit` | SKU 条码唯一、SN 管理标识、批次/效期/ABC 管理标识（标识决定出入库校验） |
| 上下游 | `wms_supplier` / `wms_customer` | 编码唯一、联系人、税率、付款账期 |
| 仓库基础 | `wms_warehouse` / `wms_area` / `wms_location` | 仓库-区域-库位三层；库位类型：拣货/存储/退货/不良品 |
| 系统 RBAC | `sys_user` / `role` / `menu` / `dept` / `user_role` / `role_menu` + 日志 2 张 | 8 张齐全；部门树；菜单树（按钮级 perms） |
| 报表 & 统计 | 视图 `v_inventory_full` / `v_purchase_reconcile` + 看板 SQL | 仓储首页看板；采购对账；出入库明细；库存余额；效期预警 TOP 50 |

---

## 10. 批量操作单据的 DTO 结构规范
**所有 "一批操作多个商品" 的单据（采购/入库/出库/调拨/销售/报损/盘点），保存请求 JSON 结构统一如下：**

```json
{
  "id": 1834792138471234560,
  "remark": "备注",
  // ============ 业务主表字段（按模块不同） ============
  "supplierId": 12,
  "purchaseNo": "PO202608240001",
  "warehouseId": 3,
  "expectDate": "2026-08-30",
  "taxRate": 13,
  "items": [
    // 一行一个商品，支持任意条数（1~500）
    {
      "itemId": null,
      "skuId": 1001,
      "lineNo": 1,
      "quantity": 100,
      "purchasePrice": 39.90,
      "taxRate": 13,
      "expectDate": "2026-08-30",
      "suggestBatch": "P260824",
      "remark": "首批"
    },
    {
      "skuId": 1005,
      "lineNo": 2,
      "quantity": 50,
      "purchasePrice": 199.00,
      "remark": null
    }
  ]
}
```

**后端保存 3 步统一流程：**
1. **DTO 基础校验**（主表必填 + items 非空 + lineNo 不重复 + skuId 存在校验）；
2. **明细 SN/批次/库位 预解析**（出库时先跑批次分配策略得到 `allocation_json`）；
3. **事务内原子保存**：主表 `INSERT/UPDATE` → 删除旧明细 → 批量 `INSERT items[]` → 写一条 `_status_log`（初始为草稿态）。

---

## 11. 事务边界与并发控制
### 11.1 事务边界
- **Service 方法 `public` + `@Transactional(rollbackFor = Exception.class)`** 一个入口；
- 同 Service 内部方法调用走 `AopContext.currentProxy()`；
- 跨模块（如采购到货回调写采购 + 写入库 + 写库存）走 `@Manager` 聚合层。

### 11.2 并发控制（无 Redis，全部走 MySQL）
| 场景 | 方案 |
|-----|------|
| 库存四要素幂等防重 | `uk_wh_sku_loc_batch` 唯一索引 + `ON DUPLICATE KEY UPDATE` |
| 同一张单据重复审核 | `SELECT ... FOR UPDATE` 行锁，状态不匹配立即抛异常（"单据已审核"） |
| 批量审核 100 张 | `WHERE id IN (...) AND status = 1` + `ORDER BY id` 加锁顺序一致，避免死锁 |
| 库存超卖 | `UPDATE wms_inventory SET quantity = quantity - ?, available_qty = available_qty - ?, version = version + 1 WHERE id = ? AND version = ? AND quantity >= ?`；受影响行数=0 则并发失败重试 3 次 |
| 同单号重复提交 | `uk_*_no` 唯一索引 |

---

## 12. 全局异常与统一响应
### 12.1 `R<T>` 统一响应体
```java
@Data
public class R<T> {
    private Integer code;       // 200 成功；10xxx 业务；401 鉴权；403 权限；404 资源；500 系统
    private String  msg;
    private T       data;
    private Long    timestamp = System.currentTimeMillis();
    // static ok() / fail() / fail(ResultCode)
}
```

### 12.2 ResultCode 枚举节选
```java
public enum ResultCode {
    SUCCESS(200, "操作成功"),
    BAD_PARAM(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或Token已过期"),
    FORBIDDEN(403, "没有该操作权限"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "系统错误"),

    // WMS 业务 10xxx
    STOCK_NOT_ENOUGH(10001, "库存不足"),
    BILL_STATUS_NOT_ALLOW(10002, "单据当前状态不允许此操作"),
    BILL_DETAIL_EMPTY(10003, "单据明细不能为空"),
    SKU_NOT_FOUND(10004, "SKU不存在"),
    BATCH_NO_REQUIRED(10005, "SKU启用批次管理，批次号必填"),
    DUPLICATE_LINE_NO(10006, "明细行号重复"),
    SOURCE_BILL_NOT_MATCH(10007, "源单与本次操作金额/数量不匹配"),
    OPTIMISTIC_LOCK_FAILED(10008, "数据已被他人修改，请刷新重试");
}
```

### 12.3 GlobalExceptionHandler
- `MethodArgumentNotValidException` → 400 + 具体字段错误；
- `BizException`（自定义）→ 10xxx 业务码；
- `DuplicateKeyException` → 400 + "唯一约束冲突"（解析出字段名）；
- `AccessDeniedException` → 403；
- 其余兜底 500 打印 ERROR 堆栈返回友好提示。

---

## 13. 审计日志 & 操作日志
### 13.1 `@OperationLog` 注解
```java
@Target(ElementType.METHOD) @Retention(RetentionPolicy.RUNTIME)
public @interface OperationLog {
    String module();   // "采购管理"
    String type();     // "新增" / "修改" / "审核"
    String detail() default "";  // 支持 SPEL："#req.purchaseNo"
    int businessType();   // 1=采购/2=入库/3=出库...
}
```
```java
@PostMapping("/save")
@PreAuthorize("hasAuthority('wms:purchase:edit')")
@OperationLog(module="采购管理", type="保存", detail="#req.purchaseNo", businessType=1)
public R<Long> save(@RequestBody @Valid PurchaseSaveReq req) { ... }
```

### 13.2 `OperationLogAspect`
- 方法正常返回 → 成功日志；方法抛异常 → 失败日志 + 堆栈写入 `sys_oper_log`；
- 自动填充：操作人、操作时间、IP、请求方法、请求参数 JSON、响应结果 JSON、耗时 ms。

### 13.3 业务状态流转审计
- 所有单据每次状态变更写 `_status_log`：`id/bill_id/bill_no/from_status/to_status/operate_by/operate_time/remark`；
- 详情页"状态时间线"直接查这张表渲染。

---

## 14. 库存核心业务逻辑（入库/出库/调拨/报损/盘点）
### 14.1 库存三量定义
| 字段 | 含义 |
|-----|------|
| `quantity` | 实存数量（物理存在） |
| `locked_qty` | 锁定数量（已分配给待出库的出库单/调拨单） |
| `available_qty` | 可用数量 = `quantity - locked_qty`（**冗余字段，Handler 维护，业务查询直接用**） |

### 14.2 统一入口：`InventoryChangeHandler` 策略
```java
public interface InventoryChangeHandler {
    /** 支持的变动类型：PURCHASE_RETURN/STOCK_IN/STOCK_OUT_LOCK/STOCK_OUT_CONFIRM/TRANSFER_OUT/TRANSFER_IN/LOSS/CHECK */
    ChangeTypeEnum type();

    /**
     * 处理一张单据的 N 条明细 → 对 wms_inventory 做 UPDATE / INSERT ON DUPLICATE KEY
     * 并逐条写入 wms_inventory_log
     * @param billId    单据ID
     * @param billNo    单据号
     * @param billTime  单据时间
     * @param items     行项（skuId / warehouseId / locationId / batchNo / qty / costPrice）
     */
    void handle(Long billId, String billNo, LocalDateTime billTime, List<? extends InventoryChangeItem> items);
}
```

### 14.3 关键 Handler 加减公式
| Handler | quantity | locked_qty | available_qty | wms_inventory_log.变化方向 |
|---------|:--------:|:----------:|:-------------:|---------------------------|
| StockIn（入库审核） | `+actual_qty` | `0` | `+actual_qty` | `+IN` |
| StockOutLock（出库"待拣货"分配批次） | `0` | `+allocated_qty` | `-allocated_qty` | `LOCK` |
| StockOutConfirm（出库审核发货） | `-actual_qty` | `-actual_qty` | `0` | `-OUT` |
| TransferOut（调拨出库） | `-actual_qty` | `0` | `-actual_qty` | `-OUT` |
| TransferIn（调拨入库） | `+actual_qty` | `0` | `+actual_qty` | `+IN` |
| Loss（报损审核） | `-loss_qty` | `0` | `-loss_qty` | `-LOSS` |
| Check 盘盈 | `+(actual-book)` | `0` | `+(actual-book)` | `+PROFIT` |
| Check 盘亏 | `-(book-actual)` | `0` | `-(book-actual)` | `-SHORT` |

> **MySQL 原子 UPDATE 模板（所有 Handler 共享）**：
> ```sql
> UPDATE wms_inventory
>    SET quantity        = quantity        + #{deltaQty},
>        locked_qty      = locked_qty      + #{deltaLocked},
>        available_qty   = available_qty   + #{deltaQty} - #{deltaLocked},
>        total_amount    = total_amount    + #{deltaAmount},
>        cost_price      = CASE WHEN quantity + #{deltaQty} = 0 THEN cost_price
>                               ELSE (total_amount + #{deltaAmount}) / (quantity + #{deltaQty}) END,
>        last_in_time    = IF(#{deltaQty} > 0, NOW(), last_in_time),
>        last_out_time   = IF(#{deltaQty} < 0, NOW(), last_out_time),
>        version         = version + 1
>  WHERE warehouse_id = #{whId} AND sku_id = #{skuId}
>    AND location_id <=> #{locId} AND batch_no <=> #{batchNo}
>    AND (quantity + #{deltaQty}) >= 0;  -- 防负库存
> ```
> 受影响行数 = 0 → 说明 **四要素不存在（需 INSERT）** 或 **负库存拦截**；分别处理。

---

## 15. 报表 & 导出（EasyExcel）
### 15.1 报表接口分层
- 简单列表（分页）：Controller → Service → Mapper `selectPage`；
- 导出 Excel（≤ 1 万行）：Controller 直接 `EasyExcel.write(response.getOutputStream(), clazz).sheet().doWrite(list)`；
- 大报表（> 1 万行）：`EasyExcel.write(XX).head(XX).registerWriteHandler(new LongestMatchColumnWidthStyleStrategy()).sheet().doWrite(分页分批查询结果流)`，不走内存聚合。

### 15.2 最小可用报表清单（v1 交付必备）
| 报表 | 筛选条件 | 导出格式 |
|-----|---------|---------|
| 采购对账（v_purchase_reconcile） | 供应商 / 日期区间 / 状态 | .xlsx |
| 入库明细 | 仓库 / 供应商 / SKU / 日期 / 批次 | .xlsx |
| 出库明细 | 仓库 / 客户 / SKU / 日期 / 出库类型 | .xlsx |
| 库存余额（v_inventory_full） | 仓库 / 区域 / 库位 / SKU / 批次效期 | .xlsx |
| 效期预警（≤ 30 天到期） | 仓库 / 剩余天数阈值 | .xlsx |
| 操作日志导出 | 模块 / 操作人 / 日期 | .xlsx |

---

## 16. 单元测试 & 部署
### 16.1 单元测试（必须覆盖）
| 测试类 | 覆盖点 | 断言 |
|-------|-------|------|
| `InventoryServiceTest` | 入库、出库、调拨、报损、盘点各一条 | 三量前后加减正确、log 表新增条数、并发 UPDATE 冲突重试成功 |
| `PurchaseFlowTest` | 采购保存→审核→入库→采购 `delivered_qty` 回写→对账视图金额一致 | 各状态流转日志 + 金额对账 |
| `DataPermissionTest` | 不同 dataScope 用户看到的行数 | 符合权限预期（看不到其他仓数据） |

### 16.2 部署（单体 Jar）
```bash
# 构建
mvn clean package -DskipTests

# 启动（生产建议 systemd 或 LaunchAgent 守护；开发直接）
java -Xms1g -Xmx2g -XX:+UseG1GC \
     -Dspring.profiles.active=prod \
     -Dspring.datasource.password=${MYSQL_PWD} \
     -Djwt.secret=${JWT_SECRET} \
     -jar wms-backend.jar
```
- API 基地址：`http://127.0.0.1:8080/wms-api`
- Knife4j 文档：`http://127.0.0.1:8080/wms-api/doc.html`

---

## 17. 未来升级为微服务的路径（可选）
如果未来业务规模（SKU > 10 万、日均出入库单 > 3000、多租户、并发 > 500），按下面 **5 步走平滑升级**，本设计预留完整分层，业务代码零迁移：

1. **引入 Nacos**：`spring-cloud-starter-alibaba-nacos-config/discovery`，将 `application.yml` 中的配置迁入 Nacos；
2. **引入 Spring Cloud Gateway**：原来的 JWT Filter / 数据权限拦截器迁入 Gateway，后端服务移除 Web 层所有鉴权逻辑；
3. **按 4 域拆 Jar**：`wms-system`（sys_* 表）、`wms-basedata`（基础资料）、`wms-core`（单据 + 库存）、`wms-report`（报表导出）；
4. **引入 RocketMQ**：单据 → 库存、单据 → 状态日志等跨服务调用改为事务消息；库存预扣改走 Redis + Lua 脚本；
5. **引入 Seata AT + Sentinel**：跨服务事务 + 接口限流熔断；MySQL 主从 + ShardingSphere 分库分表（按 `warehouse_id`）。

---

> **附**：接口级详细文档，见同目录 [WMS-API接口文档.md](./WMS-API接口文档.md)。
