# 项目启动说明
## 一、环境要求
- JDK 17+
- MySQL 8.0+
- Maven 3.6+

## 二、快速启动
### 1. 克隆代码
```bash
git clone <项目地址>
cd <项目目录>
```

### 2. 配置本地环境（关键）
#### 方式一：使用默认配置（你自己）
如果你的本地 MySQL 密码是 123456，直接启动即可，无需任何额外配置。

#### 方式二：自定义配置（其他同事）
如果你的本地 MySQL 密码不是 123456，请在启动前配置环境变量：

**Windows (CMD)**:
```cmd
set DB_PASSWORD=你的本地数据库密码
mvn spring-boot:run
```

**Windows (PowerShell)**:
```powershell
$env:DB_PASSWORD="你的本地数据库密码"
mvn spring-boot:run
```

> 注意：环境变量仅在当前终端窗口有效，关闭后需重新配置。

### 3. 自动建表
项目启动时，会自动执行 `src/main/resources/db/mysql_schema.sql` 脚本，完成数据库和表的创建。脚本中已包含 `IF NOT EXISTS`，可安全重复启动。

### 总结
1. 启动前需确保本地安装JDK 17+、MySQL 8.0+、Maven 3.6+环境；
2. 本地MySQL密码为123456可直接启动，非默认密码需先配置对应环境变量；
3. 项目启动会自动执行建表脚本，脚本具备重复执行安全性。