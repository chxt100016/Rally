# tournament.tournament-list.flow.query-tournament-admin-list 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 后台鉴权、筛选条件与分页参数如何校验？
  > POST /tournament/admin/list 受 AdminApiKeyInterceptor。JSON cmd 必须有 pageNum>=1、pageSize 1..100；status 必须可解析 TournamentStatusEnum，非法值全局失败。cityCode/ntrpLevel 非blank时原样精确筛选，无名录/格式校验。
  → 已写入接口契约、详细流程第 1-2 步及鉴权/参数分支

- [Q2] 查询排序、总数、hasMore 与超界页如何处理？
  > MyBatis Page 按 create_time DESC，无第二排序。total 为全部筛选计数；hasMore = current*size < total。无匹配或页码超界返回 list=[]、真实 total、hasMore=false，不报错。
  → 已写入详细流程第 3/6 步与空页分支

- [Q3] 列表字段、图片签名与异常时是否返回部分页？
  > 返回 TournamentAdminItemDTO 的配置摘要、状态、currentFilledSlots/createTime，不含规则与奖金。posterKey/wechatQrKey 生成3600秒签名 URL，空键应为null。DB、枚举/主题映射或签名异常终止整体 OPERATION_FAILED，不返回部分页。
  → 已写入详细流程第 4-5 步、业务活动与整体失败分支
