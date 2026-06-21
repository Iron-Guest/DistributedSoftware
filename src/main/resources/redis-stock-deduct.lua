-- 秒杀库存预扣减 Lua 脚本
-- KEYS[1]: 商品库存 key (seckill:stock:{goodsId})
-- ARGV[1]: 扣减数量
-- 返回值: 1=扣减成功, 0=库存不足

local stockKey = KEYS[1]
local quantity = tonumber(ARGV[1])

local fields = redis.call('HMGET', stockKey, 'totalStock', 'lockedStock', 'soldCount')
local totalStock = tonumber(fields[1] or 0)
local lockedStock = tonumber(fields[2] or 0)
local soldCount = tonumber(fields[3] or 0)

local available = totalStock - lockedStock - soldCount

if available >= quantity then
    redis.call('HINCRBY', stockKey, 'lockedStock', quantity)
    return 1
else
    return 0
end