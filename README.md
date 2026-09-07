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

浏览器访问 `http://127.0.0.1:5173`。开发时保持 Vite 运行，另开终端执行 `npm --prefix client start` 即可在 Electron 中打开相同的 Vue 页面。

## 验证

```sh
npm --prefix client test
npm --prefix client run build
npm --prefix client run build:electron
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env
read -rs 'BLOB_TEST_DB_PASSWORD?MySQL test password: '
export BLOB_TEST_DB_PASSWORD
mvn -f server/pom.xml test
```

测试使用 `blob_test`，开发使用 `blob_dev`。前端严格检查项目源文件，`skipLibCheck` 仅跳过第三方声明文件的内部检查。

当前已实现后端日志/标签/图片接口、Redis 降级及前端首页/依赖设置页。日志编辑界面、Electron 内置 Java 进程管理和 macOS 安装包仍在后续里程碑中；当前 `start` 是连接本地 Vite 的开发入口。
