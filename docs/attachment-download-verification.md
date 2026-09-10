# 附件下载验证指南

本指南说明如何验证附件下载行为。测试结论应记录实际版本、环境和结果，不以历史测试次数代替本次验证。

## 行为约定

- 附件通过鉴权下载接口提供，未登录请求返回 `401`。
- 优先从 `media_asset.original_name` 获取原始名称，经安全处理后写入 ASCII `filename` 与 UTF-8 `filename*`。
- UUID 继续用于 URL 和物理存储，原始名称不参与路径解析。
- 旧附件没有有效原名时使用稳定文件名；数据库查询失败返回 `500 / INTERNAL_ERROR`，不能静默作为“没有原名”处理。

## 自动化验证

按[工具链指南](java-maven-toolchain.md)配置 JDK 21，在 `backend` 目录执行：

```powershell
.\mvnw.cmd test '-Dtest=FilesApiServletTest,AttachmentDownloadNameServiceTest,UploadFileDownloadUtilTest'
```

这些测试检查下载响应、名称解析、安全编码和失败处理，不代表已经验证真实浏览器保存文件或实际数据库集成。完整后端回归可执行 `mvnw.cmd test`，无需添加 Byte Buddy experimental 参数。

## 浏览器与文件核对

优先使用已有测试附件；需要上传新样本时，使用独立测试库和临时上传根目录。

| 场景 | 核对内容 |
| --- | --- |
| 管理员、普通用户下载已有附件 | 建议下载名正确，内容可读 |
| 未登录访问 | 返回 401，不返回文件内容 |
| 中文、空格、特殊字符文件名 | 响应头编码正确，保存名经过预期安全处理 |
| 原名缺失或无效的旧附件 | 使用稳定文件名 |
| 同名不同内容附件 | 各自内容正确，URL 和存储标识不冲突 |
| 重新部署后下载 | 文件仍来自持久化目录，内容一致 |

浏览器可能为已有同名文件添加序号，也可能调整操作系统不允许的字符，需结合响应头区分浏览器行为与服务端行为。

下载后只读比较服务器原文件与下载副本：

```powershell
$sourceFile = Read-Host '请输入服务器原文件路径'
$downloadFile = Read-Host '请输入下载副本路径'
Get-Item -LiteralPath $sourceFile, $downloadFile | Select-Object Name, Length
Get-FileHash -LiteralPath $sourceFile, $downloadFile -Algorithm SHA256
```

记录实际文件名、长度及 SHA-256 是否一致。自动化浏览器被拦截时，可手动下载后核对，并明确区分自动化与手动结果。未运行的场景标记为未验证。

附件下载验证不替代[媒体生命周期](media-lifecycle-runbook.md)的数据库迁移、回填或清理验收。