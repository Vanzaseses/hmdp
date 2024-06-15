--判断传入的参数是否等于key对应的value
if(ARGV[1]==redis.call('get',KEYS[1]))then
    --是就删除
    return redis.call('del',KEYS[1])
end
--否则返回0
return 0