# hmdp
 黑马点评项目完整代码，包含前后端
技术架构： Redis + Spring Boot + MySQL + RabbitMQ + Mybatis-plus + Hutool + JWT 
实现了短信验证码登录、查找最近店铺、优惠券秒杀、关注推送、发表点评的完整业务流程
使用redis+JWT解决了在集群模式下的Session共享问题，使用拦截器实现用户的登录校验和Token刷新
使用Cache Aside模式解决数据库与缓存一致性问题
使用redis对高频访问的信息进行缓存预热，用缓存空值解决缓存穿透，用随机ttl解决缓存雪崩，用逻辑过期解决缓存击穿
使用lua脚本以及redis单线程特性解决秒杀问题，使用乐观锁解决超卖问题
使用redis分布式锁解决了在集群模式下一人一单的线程安全问题，使用RabbitMQ实现异步下单
使用ZSet数据结构实现了点赞排行榜功能，用set以及集合运算实现关注和共同关注功能

