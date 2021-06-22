package com.service.serviceImpl;

import com.entity.UserRedPacket;
import com.service.RedisRedPacketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class RedisRedPacketServiceImpl implements RedisRedPacketService {

    private static final String PREFIX = "red_packet_list_";
    //每次取出1000条记录，避免一次取出消耗太多内存
    private static final int TIME_SIZE = 1000;
    @Autowired
    private RedisTemplate redisTemplate = null;
    //数据源
    @Autowired
    private DataSource dataSource = null;
    //开启新的线程 自动创建一个线程执行 采用jdbc的批量处理，每1000条保存一次
    @Async
    @Override
    public void saveUserRedPacketByRedis(Long redPacketId, Double unitAmount) throws SQLException {
        System.out.println("开始保存数据");
        Long start = System.currentTimeMillis();
        BoundListOperations ops = redisTemplate.boundListOps(PREFIX+redPacketId);
        //数据总数
        Long size = ops.size();
        //times代表插入的批次
        Long times = size%TIME_SIZE==0?size/TIME_SIZE:size/TIME_SIZE+1;
        int count =0;
        List<UserRedPacket> userRedPacketList = new ArrayList<UserRedPacket>(TIME_SIZE);
        for(int i=0;i<times;i++){
            List userIdList = null;
            if(i==0){
                //包括第(i+1)*TIME_SIZE
                userIdList = ops.range(i*TIME_SIZE,(i+1)*TIME_SIZE);
            }else{
                userIdList = ops.range(i*TIME_SIZE+1,(i+1)*TIME_SIZE);
            }
            userRedPacketList.clear();
            for(int j=0;j<userIdList.size();j++){
                String args = userIdList.get(j).toString();
                String[] arr = args.split("-");
                String userIdStr = arr[0];//抢红包用户id
                String timeStr = arr[1];//抢红包时间
                Long userId = Long.parseLong(userIdStr);
                Long time = Long.parseLong(timeStr);
                UserRedPacket userRedPacket = new UserRedPacket();
                userRedPacket.setRedPacketId(redPacketId);
                userRedPacket.setUserId(userId);
                userRedPacket.setAmount(unitAmount);
                userRedPacket.setGrabTime(new Timestamp(time));
                userRedPacket.setNote("抢红包:"+redPacketId);
                userRedPacketList.add(userRedPacket);
            }
            count+=executeBatch(userRedPacketList);
        }
        redisTemplate.delete(PREFIX+redPacketId);
        Long end = System.currentTimeMillis();
        System.out.println("耗时："+(end-start)+"插入数量:"+count);
    }

    //批处理：这里使用Statement而不是PreparedStatement
    //https://www.cnblogs.com/tommy-huang/p/4540407.html
    //https://blog.csdn.net/zhangw1236/article/details/54583192
    // 特点：批处理是指一次性执行多条SQL语句，并且在执行过程中，如果某条语句出现错误，则仅停止该错误语句的执行，而批处理中其他所有语句则继续执行。
    //Statement:
    //优点：可以向数据库发送多条不同的ＳＱＬ语句。
    //缺点：
    //SQL语句没有预编译。
    //PreparedStatement:
    //优点：可以通过占位符预编译，简化了重复属性多条格式相同的语句。
    //缺点：执行批处理的时候只能执行同一格式类型的语句，不能混合其他语句同时执行
    private int executeBatch(List<UserRedPacket> userRedPacketList) throws SQLException {
        Connection conn = null;
        Statement stmt = null;
        int[] count = null;
        try {
            ////不会自动提交，而需要使用conn.commit()方法，手动提交事务
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            stmt = conn.createStatement();
            for (UserRedPacket userRedPacket : userRedPacketList) {
                String sql1 = "update T_RED_PACKET set stock = stock-1 where id=" + userRedPacket.getRedPacketId();
                DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String sql2 = "insert into T_USER_RED_PACKET(red_packet_id, user_id, " + "amount, grab_time, note)"
                        + " values (" + userRedPacket.getRedPacketId() + ", " + userRedPacket.getUserId() + ", "
                        + userRedPacket.getAmount() + "," + "'" + df.format(userRedPacket.getGrabTime()) + "'," + "'"
                        + userRedPacket.getNote() + "')";
                stmt.addBatch(sql1);
                stmt.addBatch(sql2);
            }
            //执行批处理命令 如果出现问题 executeBatch()就会抛出异常回滚
            count = stmt.executeBatch();
            //stmt.clearBatch() 清空addBatch，这里不需要
            for(int i=0;i<count.length;i++){
                if(count[i]==0){
                    throw new SQLException();//这里会遇到这种情况，更新库存，但是不是目标红包id，如需要更新的是id：1，结果我们跟新了id：5，即使id5
                    // 的红包不存在，也不会报错，只会返回0而已，这时候数据库还是更新了，只不过是跟新了用户信息而已。
                }
            }
            //提交事务 如果代码正常得commit之后才会操作数据库
            conn.commit();
            conn.setAutoCommit(true);//在把自动提交打开
        }catch (SQLException ex){
            conn.rollback();
            throw new RuntimeException("抢红包批量执行错误");
        }finally {
           try {
               {
                   if(stmt!=null&&!stmt.isClosed()){
                       stmt.close();
                   }
                   if(conn!=null&&!conn.isClosed()){
                       conn.close();
                   }
               }
           }catch (Exception ex){
               ex.printStackTrace();
           }
        }
        return count.length/2;
    }
}
