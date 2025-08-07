# 邮件搜索引擎 (V1.0)

这是一个基于 **Spring Boot 3** 构建的、技术栈全面的企业级应用基础框架。项目以“内部邮件搜索”为业务场景，深度集成了 **MySQL**, **Elasticsearch**, **Redis**, **MyBatis-Plus**, **Druid** 等主流后端技术。

这个项目不仅是一个功能完整的搜索引擎，更是一个可以作为你任何新项目起点的**“黄金脚手架”**。

## 架构设计 (V1.0)

本项目采用读写分离、数据解耦的现代架构：

1.  **数据抓取与持久化:**
    * `MailFetchingService` 定时连接IMAP服务器，抓取新邮件。
    * 邮件数据被持久化到 **MySQL** 数据库中，作为系统的“真实数据源”(Source of Truth)。

2.  **数据索引:**
    * `IndexingService` 定时从MySQL中读取新增或更新的邮件数据。
    * 将数据同步到 **Elasticsearch** 中，建立全文检索引擎。

3.  **数据查询与缓存:**
    * 用户通过Web界面发起搜索请求。
    * `SearchService` 优先查询 **Redis** 缓存。
    * 若缓存未命中，则查询Elasticsearch，并将结果写入Redis缓存，再返回给用户。

![Architecture Diagram](https://your-image-url.com/architecture.png)  ## 核心技术栈

* **后端:** Spring Boot 3.2.x (Java 17)
* **数据库:** MySQL 8.x
* **连接池:** Druid
* **ORM:** MyBatis-Plus
* **搜索引擎:** Elasticsearch 8.x
* **缓存:** Redis
* **邮件抓取:** Jakarta Mail
* **前端模板:** Thymeleaf

## 快速开始

### 1. 环境准备

* **JDK 17** 或更高版本。
* **Maven 3.8** 或更高版本。
* **Docker** (推荐)。

### 2. 启动依赖服务

本项目依赖MySQL, Elasticsearch和Redis。使用Docker可以一键启动。

```bash
# 启动 Elasticsearch (并安装IK分词器)
docker run -d --name es01 -p 9200:9200 -p 9300:9300 -e "discovery.type=single-node" -e "xpack.security.enabled=false" docker.elastic.co/elasticsearch/elasticsearch:8.14.1
docker exec -it es01 ./bin/elasticsearch-plugin install [https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.14.1/elasticsearch-analysis-ik-8.14.1.zip](https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v8.14.1/elasticsearch-analysis-ik-8.14.1.zip)
docker restart es01

# 启动 MySQL 8
docker run -d --name mysql8 -p 3306:3306 -e MYSQL_ROOT_PASSWORD=your_mysql_password -e MYSQL_DATABASE=mail_search mysql:8.0

# 启动 Redis
docker run -d --name redis -p 6379:6379 redis:latest
```
**注意:** 将上面MySQL的 `your_mysql_password` 替换为你自己的密码。

### 3. 初始化数据库

连接到你刚启动的MySQL实例，并执行以下SQL语句来创建表：

```sql
CREATE DATABASE IF NOT EXISTS mail_search;
USE mail_search;

CREATE TABLE `emails` (
  `id` bigint NOT NULL,
  `message_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `from_address` varchar(255) DEFAULT NULL,
  `subject` varchar(500) DEFAULT NULL,
  `content` text,
  `sent_date` datetime DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_message_id` (`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
```

### 4. 配置项目

打开 `src/main/resources/application.yml` 文件，**仔细检查并修改**所有标记了注释的配置项，特别是数据库密码和你的邮箱信息。

### 5. 运行项目

在项目根目录下，使用Maven运行项目。

```bash
mvn spring-boot:run
```

### 6. 访问和使用

打开浏览器，访问 `http://localhost:8080`。

项目启动后会立即执行一次邮件抓取任务，将邮件存入MySQL，随后索引服务会将数据同步到Elasticsearch。整个过程可能需要一两分钟。之后你就可以开始使用功能强大的搜索引擎了！
