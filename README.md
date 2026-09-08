# Blob

Blob 是一个面向个人使用的本地桌面知识日志应用，用来记录学习与生活、沉淀 Markdown 知识，并逐步练习 Vue、Java、MySQL、Redis、Electron 与 AI/RAG 技术。

## M1 范围

- Markdown 学习/生活日志
- 标签管理与关键词检索
- 图片上传并插入 Markdown
- MySQL 持久化
- Redis 缓存与搜索词统计（不可用时自动降级）
- macOS arm64 Electron 桌面应用

详细设计与实施步骤位于 `docs/superpowers/`。

## 项目目录

```text
Blob/
├── client/          # 前端工程，统一管理 Vue 与 Electron 依赖
│   ├── src/         # Vue 渲染层、页面、接口和状态管理
│   ├── electron/    # Electron 主进程
│   └── tests/       # 前端测试
├── server/          # Spring Boot 后端，SDKMAN 管理 Java/Maven
└── storage/images/  # 本地图片
```

## 本地开发

Java 21 与 Maven 3.9.15 通过 SDKMAN 管理，版本见 `.sdkmanrc`；Node 24 通过 nvm 管理，版本见 `.nvmrc`。

启动本地依赖：

```sh
brew services start mysql
brew services start redis
mysql -h 127.0.0.1 -u root -p
```

首次连接后创建开发库和独立测试库（不会覆盖已有库）：

```sql
CREATE DATABASE IF NOT EXISTS blob_dev CHARACTER SET utf8mb4;
CREATE DATABASE IF NOT EXISTS blob_test CHARACTER SET utf8mb4;
```

在项目根目录的 zsh 终端启动后端，密码通过隐藏输入传入环境变量：

```sh
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
read -rs 'BLOB_DB_PASSWORD?MySQL password: '
export BLOB_DB_PASSWORD
mvn -f server/pom.xml spring-boot:run
```

另开终端启动 Vue：

```sh
nvm use
npm --prefix client ci
npm --prefix client run dev
```

浏览器访问 `http://127.0.0.1:5173`。Electron 开发模式需要先完成下文的资源准备，再保持 Vite 运行，另开终端执行 `npm --prefix client start`。桌面首次进入设置页，填写数据库与 Redis 配置；桌面会启动自己的随机端口 Java 子进程，不依赖手工启动的 8080 服务。

## macOS 安装与打包

安装包位于 `client/out/make/Blob.dmg`（Apple Silicon / arm64）。打开 DMG，把 Blob 拖到 Applications 后启动。

- 安装包包含 Vue 页面、Spring Boot JAR 和 Java 21，无需另外安装 Java/Maven/Node。
- MySQL 和 Redis **不在安装包里**，仍由 Homebrew 管理。先创建 `blob_dev`，然后在应用设置中填写连接信息；Flyway 自动建表。
- 密码使用 macOS `safeStorage` 加密保存，不会回显；修改配置时密码留空表示保留原密码。
- 个人本机构建未配置 Developer ID 签名和公证，不应作为已签名发行版本分发。若系统拦截，在“系统设置 → 隐私与安全性”中明确允许本次本地构建；不要关闭全局 Gatekeeper。
- 日志正文和标签在 MySQL；图片、加密配置、后端滚动日志在 `~/Library/Application Support/Blob/blob-data/`。备份时同时备份 MySQL 与这个目录。卸载 `.app` 不等于删除个人数据。
- Redis 不可用时回退 MySQL，近期搜索可能暂不可用；MySQL 不可用时设置页会提示连接失败。

从源码重新打包（项目根目录）：

```sh
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
nvm use
read -rs 'BLOB_TEST_DB_PASSWORD?MySQL test password: '
export BLOB_TEST_DB_PASSWORD
export BLOB_JAVA_RUNTIME_DIR="$JAVA_HOME"
npm --prefix client run make
npm --prefix client run verify:package
```

`make` 会依次构建前端、Electron、后端（含真实 MySQL 测试），验证并复制 arm64 Java 21 资源，再生成 `.app`、`.dmg` 与 `.zip`。资源准备可单独执行 `npm --prefix client run prepare:resources`，但需要已有前后端构建产物。

## 验证

```sh
npm --prefix client test
npm --prefix client run build
npm --prefix client run build:electron
npm --prefix client run test:desktop
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
read -rs 'BLOB_TEST_DB_PASSWORD?MySQL test password: '
export BLOB_TEST_DB_PASSWORD
mvn -f server/pom.xml test
```

测试使用 `blob_test`，开发使用 `blob_dev`。前端严格检查项目源文件，`skipLibCheck` 仅跳过第三方声明文件的内部检查。

浏览器端到端测试使用独立 `blob_test` 后端（18080）和临时 Vite（5174）：

```sh
# 终端一：先按上文载入 SDKMAN，并隐藏输入、导出 BLOB_DB_PASSWORD
export BLOB_DB_URL='jdbc:mysql://127.0.0.1:3306/blob_test?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai'
export BLOB_SERVER_PORT=18080
export BLOB_DEV_ORIGIN=http://127.0.0.1:5174
mvn -f server/pom.xml spring-boot:run

# 终端二：使用本机 Chrome；未安装 Chrome 时先安装 Playwright Chromium 并省略 channel
BLOB_E2E_CHANNEL=chrome npm --prefix client run e2e
```

真实安装包验收会只读挂载 DMG、复制到临时目录，使用独立用户目录和 `blob_test`，测试后清理测试日志，不覆盖 Applications 中已有应用：

```sh
read -rs 'BLOB_SMOKE_DB_PASSWORD?MySQL test password: '
export BLOB_SMOKE_DB_PASSWORD
node client/scripts/smoke-package.mjs
```

M1 不包含 AI 摘要、标签推荐、向量检索、学习计划和成长统计；这些保留到后续渐进开发阶段。

本次 M1 验收结果与已知边界见 [验收记录](docs/testing/m1-acceptance.md)。Redis 停机测试脚本需要显式同意后再运行，它会短暂停止本机 Homebrew Redis 并在结束时恢复，不要在其他项目依赖 Redis 的关键操作期间执行。
