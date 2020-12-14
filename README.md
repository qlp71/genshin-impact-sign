## 原神米游社自动签到

### 说明

每天凌晨4点自动执行签到任务, 支持同时签到多个账号

### 使用方法

浏览器打开开发者工具(F12) 并进入米游社登录账号: https://bbs.mihoyo.com/ys/

复制该网页返回的cookie

![image-20201214082418075](docs\cookie.png)

把cookie粘贴到src/main/resources/cookies.txt文件下, 如果有多个账号需要签到, 每个cookie占一行即可

