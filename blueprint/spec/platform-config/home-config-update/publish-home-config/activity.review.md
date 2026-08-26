# platform-config.home-config-update.activity.publish-home-config 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 首页更新入口允许哪些 key，布局区域规则是什么？
  > 只允许三项首页key；布局数组最多30区，id必填唯一且格式长度受限，type限六种，除POSTER外同类唯一。
  → 已写入业务动作 A1、详细流程第 1-2 步

- [Q2] 海报配置和海报项的校验规则是什么？
  > POSTER区需标题和数组；赛事海报需标题/副标题/数组；通用配置为数组。每数组最多20项，item type仅NAVIGATE/PREVIEW且image必填，空数组允许。
  → 已写入详细流程第 2-3 步与边界情况

- [Q3] 版本发布和缓存刷新如何处理？
  > 首次version0建version1，已有以id+version条件更新、重新启用并加一；当前JVM缓存事务内重建但不跨实例、数据库回滚也不补偿。
  → 已写入业务动作 A2-A3、详细流程第 4-5 步与实现提示
