if  redis.call('EXISTS',KEYS[1])==1
then
    redis.call('set',KEYS[1],ARGV[2])
else
    redis.call('set',KEYS[1],ARGV[1])
end
return  redis.call('get',KEYS[1])