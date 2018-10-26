
### 本地消息事务中间件
- bedt : 最大努力通知柔性事务(Best Effort Delivery Transaction)
- ctp  : 事务补偿模式(Compensating Transaction pattern)
  - [微软azure架构ctp理论](https://docs.microsoft.com/en-us/azure/architecture/patterns/compensating-transaction)
  - [微软azure架构ctp理论中文版](https://iambowen.gitbooks.io/cloud-design-pattern/categories/availability.html)
- tcc  : 尝试确认取消事务(Try-Confirm-Cancel transaction)


### 待解决问题
- 具体实现服务的数据层(memory)实现
- 具体实现服务的数据层(mysql)实现
- 好的demo

### 解决
- 支持tcc模式
- 消费端的幂等性封装
- 事件执行异常的error信息存储

### 你所需要做的事情
1. config 配置并初始化 TransactionFactory.init()
2. 微服务实现自己项目所需的 TransactionDataProvider 数据存储器,
3. 根据你的存储器可能要建mysql对应表. (⊙o⊙)…
4. 编写各自 bedt/ctp/tcc 所需event类业务
5. 编写远程调用端幂等业务 
6. …… 开始玩耍吧

--------------------------------------------------

### bedt : 最大努力通知柔性事务(Best Effort Delivery Transaction)
- 事件状态: 默认状态(不该存在状态), 
    - BEDT_EXECUTING    : 执行中 
    - BEDT_FINISH       : 执行完成
    - BEDT_ERROR        : 执行最终异常

```text
事务server端
begin transaction (开始本地事务T)
  主体local业务执行
  save all BEDT events(mysql | memory)(status: BEDT_EXECUTING; retry:0)
commit transaction (提交本事事务T)

远程业务1执行(event 1执行)
  success : 执行成功, 更新(status: BEDT_FINISH)
  fail    : 执行异常, 更新retry += 1, errorInfo

远程业务2执行(event 2执行)
  success : 执行成功, 更新(status: BEDT_FINISH)
  fail    : 执行异常, 更新retry += 1, errorInfo

远程业务n执行(event n执行)
  success : 执行成功, 更新(status: BEDT_FINISH)
  fail    : 执行异常, 更新retry += 1, errorInfo



事务server端 task(定时扫描)
查找BEDT events (status: BEDT_EXECUTING)
  远程业务执行(event 执行)
    success : 更新(status: BEDT_FINISH)
    fail    : 更新retry += 1, errorInfo
  event retry超过最大次数: (status: BEDT_ERROR)
  
```

--------------------------------------------------

### ctp  : 事务补偿模式(Compensating Transaction pattern)
- 状态: 默认状态(不该存在状态), 
    - CTP_INIT              : 初始化
    - CTP_FINISH            : 执行完成 
    - CTP_ERROR             : 执行异常 
    - CTP_COMPENSATING      : 补偿中
    - CTP_COMPENSATE_FINISH : 补偿完成 
    - CTP_COMPENSATE_ERROR  : 补偿最终异常

```text
事务server端
save CTP event(mysql | memory)(status: CTP_INIT)
业务1(local|远程)执行(event执行)
  fail : 
    更新event(status: CTP_ERROR, errorInfo)
    异步执行task uuid补偿操作(task-2)
    结束流程

save CTP event(mysql | memory)(status: CTP_INIT)
业务2(local|远程)执行(event执行)
  fail : 
    更新event(status: CTP_ERROR, errorInfo) 
    异步执行task uuid补偿操作(task-2)
    结束流程

save CTP event(mysql | memory)(status: CTP_INIT)
业务n(local|远程)执行(event执行)
  fail : 
    更新event(status: CTP_ERROR, errorInfo)
    异步执行task uuid补偿操作(task-2)
    结束流程

begin transaction (开始本地事务T)
  other local业务(如: update起始事件中业务数据状态 不可见=>可见.)
  update all uuid events status: CTP_FINISH
commit transaction (提交本事事务T)



事务server端 - task(定时扫描)
  1. 查找events (status: CTP_INIT) & 时间超出有效范围(5分钟) => 标记status: CTP_COMPENSATING
  2. 查找events (status: CTP_COMPENSATING)
    执行event补偿业务
      success : 更新(status: CTP_COMPENSATE_FINISH)
      fail    : 更新retry += 1, errorInfo, status: CTP_COMPENSATING
      超过最大重试次数 : 更新(status: CTP_COMPENSATE_ERROR)
      
```

--------------------------------------------------

### tcc  : 尝试确认取消事务(Try-Confirm-Cancel transaction)
- 状态: 默认状态(不该存在状态), 
    - TCC_TRYING            : 尝试中
    - TCC_TRY_ERROR         : 尝试异常
    - TCC_CONFIRMING        : 提交中
    - TCC_CONFIRM_FINISH    : 提交完成
    - TCC_CONFIRM_ERROR     : 提交最终异常
    - TCC_CANCELING         : 取消中
    - TCC_CANCEL_FINISH     : 取消完成
    - TCC_CANCEL_ERROR      : 取消最终异常

```text
事务server端
save TCC event(mysql | memory)(status: TCC_TRYING)
业务1(local|远程)执行try(event执行 try)
  fail : 
    更新event(status: TCC_TRY_ERROR, errorInfo) 
    update uuid events status: TCC_CANCELING
    异步执行task uuid取消操作(task-2)
    结束流程

save TCC event(mysql | memory)(status: TCC_TRYING)
业务2(local|远程)执行try(event执行 try)
  fail : 
    更新event(status: TCC_TRY_ERROR, errorInfo) 
    update uuid events status: TCC_CANCELING
    异步执行task uuid取消操作(task-2)
    结束流程

save TCC event(mysql | memory)(status: TCC_TRYING)
业务n(local|远程)执行try(event执行 try)
  fail : 
    更新event(status: TCC_TRY_ERROR, errorInfo) 
    update uuid events status: TCC_CANCELING
    异步执行task uuid取消操作(task-2)
    结束流程

begin transaction (开始本地事务T)
  update all uuid events status: Confirm提交中
  异步执行task uuid提交业务(task-3)
commit transaction (提交本事事务T)



事务server端 - task(定时扫描)
  1. 查找events(status: TCC_TRYING) & 时间超出有效范围(5分钟) => 标记status: TCC_CANCELING
  2. 查找events(status: TCC_CANCELING)
    执行event取消业务
      success : 更新(status: TCC_CANCEL_FINISH)
      fail    : 更新retry += 1, errorInfo status: TCC_CANCELING
      超过最大重试次数 : 更(status: TCC_CANCEL_ERROR)
  3. 查找status = TCC_CONFIRMING 提交中 events
    执行event提交业务
      success : 更新(status: TCC_CONFIRM_FINISH)
      fail    : 更新retry += 1, errorInfo  status: TCC_CONFIRMING
      超过最大重试次数 : 更新(status: TCC_CONFIRM_ERROR)
      
```


--------------------------------------------------
### 幂等性实践

```text
事务client端 - Cancel
  begin transaction (开始本地事务T)
    1. 尝试写入(幂等key, 当前时间, status: INIT) 已存在则会fail
      fail    : 获得幂等key数据. json还原现场数据 并 返回成功,结束流程
      success : 进入流程 2
    2. 执行本地业务逻辑
      fail    : 抛出异常,所有数据回滚.
      success : 进入流程 3
    3. 如果存在返回数据, update 幂等key现场数据 并返回
  commit transaction (提交本事事务T)
```