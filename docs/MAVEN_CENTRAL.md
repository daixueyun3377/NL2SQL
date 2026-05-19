# 发布到 Maven Central

版本 **1.0.0** 使用 [Sonatype Central Publisher](https://central.sonatype.org/publish/publish-portal-gradle/) 与 `central-publishing-maven-plugin`。

## 前置条件

1. 在 [central.sonatype.com](https://central.sonatype.com/) 注册并验证命名空间 `io.github.daixueyun3377`（GitHub 仓库验证）。
2. 生成 Central 用户 Token（Settings → Account）。
3. 本机安装并配置 **GPG**，公钥上传到 keyserver.ubuntu.com。

## ~/.m2/settings.xml 示例

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username><!-- Central 用户名 --></username>
      <password><!-- Central Token --></password>
    </server>
  </servers>

  <profiles>
    <profile>
      <id>gpg</id>
      <properties>
        <gpg.keyname><!-- 你的 GPG Key ID --></gpg.keyname>
      </properties>
    </profile>
  </profiles>

  <activeProfiles>
    <activeProfile>gpg</activeProfile>
  </activeProfiles>
</settings>
```

## 发布命令

在仓库根目录（已检出 `v1.0.0` 标签）：

```bash
mvn clean deploy -Prelease
```

`-Prelease` 会启用：源码包、Javadoc、GPG 签名、Central Publisher 上传。

## 发布产物

| artifactId | 类型 |
|--------------|------|
| `nl2sql-parent` | pom |
| `nl2sql-bom` | pom |
| `nl2sql-core` | jar |
| `nl2sql-spring-boot-starter` | jar |

发布后可在 [central.sonatype.com](https://central.sonatype.com/) 或 [search.maven.org](https://search.maven.org/) 搜索 `io.github.daixueyun3377`。
