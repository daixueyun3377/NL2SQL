# NL2SQL 语义层（默认示例，应用方请复制 templates 后本地维护）

## default

domain: default
keywords:
tables:

### relation_hints

### business_rules

### examples

## job

domain: job
keywords: 岗位, 招聘, job, 职位
tables: job, job_address, job_basic_info, city, brand

### relation_hints

按城市查岗位：job JOIN job_address ON job.id = job_address.job_id JOIN city ON job_address.city_id = city.id

### business_rules

1. 只能使用下面出现的表
2. 软删除：job.delete_at IS NULL，job_address.delete_at IS NULL，city.delete_at IS NULL；job_basic_info.is_deleted = 0
3. 按城市查岗位：job JOIN job_address ON job.id = job_address.job_id JOIN city ON job_address.city_id = city.id
4. 必须加 LIMIT，不超过 100 行

### examples

question: 有多少在招岗位

```sql
SELECT COUNT(*) AS cnt FROM job_basic_info WHERE status = 1 AND is_deleted = 0 LIMIT 100
```
