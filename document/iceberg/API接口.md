常用 REST 接口（设计建议）

命名空间（Namespace）相关
GET /api/v1/namespaces — 列出所有命名空间
GET /api/v1/namespaces/{namespace} — 获取命名空间元数据（若需要）
POST /api/v1/namespaces/{namespace} — 创建命名空间（可带 properties）
DELETE /api/v1/namespaces/{namespace} — 删除命名空间
表（Table）相关
GET /api/v1/tables — 列出所有表（或加 namespace 过滤）
GET /api/v1/tables/{namespace}/{table} — 获取表元数据（schema、partition、location、properties、snapshots）
POST /api/v1/tables/{namespace}/{table} — 创建表（需要 schema/partition/properties）
DELETE /api/v1/tables/{namespace}/{table} — 删除表（可选 purge 参数）
PUT /api/v1/tables/{namespace}/{table} — 修改表属性（update properties）
POST /api/v1/tables/{namespace}/{table}/rename — 重命名表
GET /api/v1/tables/{namespace}/{table}/snapshots — 列出快照
POST /api/v1/tables/{namespace}/{table}/rollback — 基于 snapshotId 回滚（可选）
数据写入/读取（通常不是通过 Catalog 完成，而是通过 Iceberg Table 的写入 API 或查询引擎）
POST /api/v1/tables/{ns}/{table}/append — 接收数据（或数据文件路径）并 append（需实现具体写入逻辑）
POST /api/v1/tables/{ns}/{table}/overwrite — 覆盖写入
GET /api/v1/tables/{ns}/{table}/manifest — 获取 manifest / metadata 信息
其它管理接口
GET /api/v1/health or /api/v1/version