# base-springboot

## SpringBoot开发模版

### 功能
* 注册、登录、用户详情、接口日志、ip属地、发送邮件、修改密码、找回密码、用户权限、角色、菜单

***
### 准备
* redis
* mysql

***
### 使用
* 数据库新建utf8mb4数据库，运行db文件夹下的db.sql文件。
* 更改application-dev.yml中mysql数据库连接配置、redis连接配置


***
### 框架选型:
<p>
    &nbsp;&nbsp;&nbsp;&nbsp;SpringBoot + Mybatis-plus + Redis <br>
    &nbsp;&nbsp;&nbsp;&nbsp;日志追踪框架 TLog  <a href="https://tlog.yomahub.com/pages/5b7bd2/" style="text-decoration: none">框架文档</a> <br>
    &nbsp;&nbsp;&nbsp;&nbsp;登录框架 Sa-Token <a href="https://sa-token.cc/doc.html#/" style="text-decoration: none">框架文档</a>
</p>

#### 项目结构:
```
    base:
        -- common:
            -- common-core
            -- common-framework
        -- business:
```
common为公共包,包含core和framework两个子包:
<p>
    &nbsp;&nbsp;&nbsp;&nbsp;common-core主要是自定义注解、constant、枚举类、自定义异常、properties、utils等
</p>
<p>
    &nbsp;&nbsp;&nbsp;&nbsp;common-framework主要是filter、listener、日志打印、日志记录、Mybatis-plus配置、统一异常处理等
</p>
<p>
    &nbsp;&nbsp;&nbsp;&nbsp;business为业务包,用来处理业务。不赘述。
</p>

****
#### 自定义注解:

@ResponseResult

<p>
    &nbsp;&nbsp;&nbsp;&nbsp;用来封装返回值,可放在controller上,或者controller中的单个方法上。
</p>

@OperationLog

<p>
    &nbsp;&nbsp;&nbsp;&nbsp;用来记录接口日志,包含请求接口名称、用户id、ip、入参、异常信息等,可放在controller中的单个方法上。
</p>

@ReleaseToken

<p>
    &nbsp;&nbsp;&nbsp;&nbsp;用来跳过token校验,可放在controller中的单个方法上。
</p>

@EncryptedField
<p>
    &nbsp;&nbsp;&nbsp;&nbsp;加在类字段上,mybatis在插入或更新时默认会自动加密该字段,查询时默认会自动解密该字段。
</p>

@SearchDate
<p>
    &nbsp;&nbsp;&nbsp;&nbsp;加在Date类型上，搜索条件开始时间结束时间，自动拼接开始时间00:00:00和结束时间23:59:59
</p>

***
#### 常用utils:

LoginUtils
<p>
    &nbsp;&nbsp;&nbsp;&nbsp;登录相关utils,可获取用户基本信息,角色,token等
</p>

ApiAssert
<p>
    &nbsp;&nbsp;&nbsp;&nbsp;断言工具类,以断言方式抛出自定义异常
</p>

EnuUtils
<p>
    &nbsp;&nbsp;&nbsp;&nbsp;枚举工具类,判断指定code是否属于指定枚举类中的数据
</p>

ApplicationUtils
<p>
    &nbsp;&nbsp;&nbsp;程序工具类,可获取SpringBean,程序上下文等
</p>

TreeUtil
<p>
    &nbsp;&nbsp;&nbsp;通用树状工具类
</p>