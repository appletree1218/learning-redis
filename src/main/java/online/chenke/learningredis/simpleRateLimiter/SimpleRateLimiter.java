package online.chenke.learningredis.simpleRateLimiter;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Response;

/**
 * 简单限流的实现
 */
public class SimpleRateLimiter {
    private Jedis jedis;

    public SimpleRateLimiter(Jedis jedis){
        this.jedis = jedis;
    }

    public boolean isActionAllowed(int period, int maxCount, String actionKey){
        long now = System.currentTimeMillis();
        Pipeline pipelined = this.jedis.pipelined();
        pipelined.multi();
        pipelined.zadd(actionKey, now, String.valueOf(now));
        pipelined.zremrangeByScore(actionKey, 0, now-period*1000);
        Response<Long> zcard = pipelined.zcard(actionKey);
        pipelined.expire(actionKey, period);
        pipelined.exec();
        pipelined.close();

        return zcard.get() <= maxCount;
    }

    public static void main(String[] args) throws InterruptedException {
        int count = 50;

        Jedis jedis = new Jedis("www.chenke.online", 9673);
        jedis.auth("chenke1234.");

        for (int i = 0; i < count; i++) {
            SimpleRateLimiter simpleRateLimiter = new SimpleRateLimiter(jedis);
            System.out.println(i+": "+simpleRateLimiter.isActionAllowed(5, 10, "test"));
        }
    }
}
