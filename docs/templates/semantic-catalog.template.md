# NL2SQL 语义层目录模板

> **用法**：复制到应用工程，例如 `src/main/resources/nl2sql/semantic-catalog.md`，按需修改后无需发版即可生效（改完重启或配合热加载策略）。
>
> **配置**：
> ```yaml
> nl2sql:
>   semantic:
>     provider: markdown
>     markdown-resource: nl2sql/semantic-catalog.md
>     # 或使用本地文件路径（优先级高于 classpath）：
>     # markdown-file: ./config/nl2sql/semantic-catalog.md
> ```

---

## default

domain: default
keywords:
tables:

### relation_hints

（默认域：未命中其他域时使用，可留空）

### business_rules

### examples

---

## job

domain: job
keywords: 岗位, 招聘, job, 职位
tables: job, job_address, job_basic_info, city, brand

### relation_hints

- 按城市查岗位：`job` JOIN `job_address` ON `job.id` = `job_address.job_id` JOIN `city` ON `job_address.city_id` = `city.id`

### business_rules

1. 只能使用本域 `tables` 列出的表
2. 软删除：`job.delete_at IS NULL`，`job_address.delete_at IS NULL`，`city.delete_at IS NULL`；`job_basic_info.is_deleted = 0`
3. 在招口径：`job_basic_info.status = 1`（按业务确认后修改）

### examples

question: 有多少在招岗位

```sql
SELECT COUNT(*) AS cnt FROM job_basic_info WHERE status = 1 AND is_deleted = 0
```

question: 上海有多少在招岗位

```sql
SELECT COUNT(DISTINCT j.id) AS cnt
FROM job j
JOIN job_address ja ON j.id = ja.job_id AND ja.delete_at IS NULL
JOIN city c ON ja.city_id = c.id AND c.delete_at IS NULL
JOIN job_basic_info jbi ON j.id = jbi.job_id AND jbi.is_deleted = 0 AND jbi.status = 1
WHERE c.name LIKE '%上海%'
LIMIT 100
```
