---
id: content-production.pending-translation-batch
---

## 服务边界

本服务处理触发时全部待译条目，分批调用 DeepSeek，保存非空译文并交付成功条数。它不发现或登记缺译内容，不翻译临时输入，不向调用者交付具体译文，不审核译文质量，也不修改原始业务资料。
