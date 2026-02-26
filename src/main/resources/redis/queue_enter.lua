-- KEYS[1] = zsetKey
-- KEYS[2] = seqKey
-- KEYS[3] = ticketKey(token)
-- KEYS[4] = rateLimitKey(nowSec)
--
-- ARGV[1] = token
-- ARGV[2] = nowMs
-- ARGV[3] = maxQueueDepth
-- ARGV[4] = maxEnqueuePerSec
-- ARGV[5] = rateKeyTtlSec
-- ARGV[6] = userId
-- ARGV[7] = productId
-- ARGV[8] = quantity

local depth = redis.call('ZCARD', KEYS[1])
if tonumber(depth) >= tonumber(ARGV[3]) then
  return { "ERR", "QUEUE_FULL", tostring(depth) }
end

local cnt = redis.call('INCR', KEYS[4])
if tonumber(cnt) == 1 then
  redis.call('EXPIRE', KEYS[4], tonumber(ARGV[5]))
end
if tonumber(cnt) > tonumber(ARGV[4]) then
  return { "ERR", "RATE_LIMIT", tostring(cnt) }
end

local seq = redis.call('INCR', KEYS[2])
redis.call('ZADD', KEYS[1], seq, ARGV[1])

redis.call('HSET', KEYS[3],
  'status', 'WAITING',
  'userId', ARGV[6],
  'productId', ARGV[7],
  'quantity', ARGV[8],
  'seq', tostring(seq),
  'createAt', ARGV[2]
)

-- OK, seq
return { "OK", tostring(seq) }