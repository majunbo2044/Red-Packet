package com.service.serviceImpl;

import com.dao.RedPacketDao;
import com.dao.UserRedPacketDao;
import com.entity.RedPacket;
import com.entity.UserRedPacket;
import com.service.RedisRedPacketService;
import com.service.UserRedPacketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import redis.clients.jedis.Jedis;

@Service
public class UserRedPacketServiceImpl implements UserRedPacketService {


    @Autowired
    private UserRedPacketDao userRedPacketDao = null;
    @Autowired
    private RedPacketDao redPacketDao = null;

    //直接保存到数据库，操作缓慢并且容易超发（数据不一致）
    private static final int FAILED =0;
    @Transactional(isolation = Isolation.READ_COMMITTED,propagation = Propagation.REQUIRED)
    public int grapRedPacket(Long redPacketId, Long userId) {
        //获取红包信息
        RedPacket redPacket = redPacketDao.getRedPacket(redPacketId);
        //红包库存大于0
        if(redPacket.getStock()>0){
            redPacketDao.decreaseRedPacket(redPacketId);
            UserRedPacket userRedPacket = new UserRedPacket();
            userRedPacket.setRedPacketId(redPacketId);
            userRedPacket.setUserId(userId);
            userRedPacket.setAmount(redPacket.getAmount());
            userRedPacket.setNote("抢红包:"+redPacketId);
            int result = userRedPacketDao.grapRedPacket(userRedPacket);
            return result;
        }
        return FAILED;
    }

    @Autowired
    private RedisRedPacketService redisRedPacketService= null;
    @Autowired
    private RedisTemplate redisTemplate = null;
    //Lua脚本
    String script = "local listKey = 'red_packet_list_'..KEYS[1] \n"
            + "local redPacket = 'red_packet_'..KEYS[1] \n"
            + "local stock = tonumber(redis.call('hget', redPacket, 'stock')) \n"
            + "if stock <= 0 then return 0 end \n"
            + "stock = stock -1 \n"
            + "redis.call('hset', redPacket, 'stock', tostring(stock)) \n"
            + "redis.call('rpush', listKey, ARGV[1]) \n"
            + "if stock == 0 then return 2 end \n"
            + "return 1 \n";
    //在缓存Lua脚本后，使用该变量保存Redis返回的SHA1编码，让它去执行缓存的Lua脚本
    String shal = null;
    //使用redis配合Lua实现抢红包
    @Override
    public Long grapRedPacketByRedis(Long redPacketId, Long userId) {
        //当前抢红包的用户和信息
        String args =userId +"-"+System.currentTimeMillis();
        Long result = null;
        //获取底层Redis操作对象
        Jedis jedis = (Jedis) redisTemplate.getConnectionFactory().getConnection().getNativeConnection();
        try {
            if(shal ==null){
                //返回shal编码
                shal = jedis.scriptLoad(script);
            }
            //执行脚本，返回结果
            Object res = jedis.evalsha(shal,1,redPacketId+"",args);
            result = (Long) res;
            //返回2时表示最后一个红包，此时抢红包的信息会通过异步保存到数据库中
            if(result==2){
                String unitAmountStr = jedis.hget("red_packet_"+redPacketId,"unit_amount");
                Double unitAmount = Double.parseDouble(unitAmountStr);
                System.out.println("thread_name:"+Thread.currentThread().getName());
                redisRedPacketService.saveUserRedPacketByRedis(redPacketId,unitAmount);
            }
        }catch (Exception ex){
            ex.printStackTrace();
        }finally {
            if(jedis!=null&&jedis.isConnected()){
                jedis.close();
            }
        }
        return result;
    }
}
