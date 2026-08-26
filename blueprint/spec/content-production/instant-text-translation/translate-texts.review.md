# content-production.instant-text-translation.flow.translate-texts 澄清记录

<!-- 本文件由 bp review 命令维护,请勿手工编辑 -->

## 待答复

## 已答复

## 已确认

- [Q1] 即时翻译是否拆批或限制每批数量？
  > 按 Java 实现确认：不拆批且没有数量或总长度上限，整份请求一次交给 DeepSeek。
  → 已落入触发和详细流程。

- [Q2] DeepSeek 返回条数与输入不一致时是否部分交付？
  > 按 Java 实现确认：不部分交付，整批返回无译文结果。
  → 已落入流程图、详细流程和异常分支。

- [Q3] 即时翻译是否保存译文或登记翻译条目？
  > 按 Java 实现确认：均不保存；相同请求重复到达会重新调用 DeepSeek。
  → 已落入触发、详细流程和服务边界。
