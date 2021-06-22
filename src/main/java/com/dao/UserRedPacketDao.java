package com.dao;

import com.entity.UserRedPacket;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRedPacketDao {
    public int grapRedPacket(UserRedPacket userRedPacket);//插入抢红包信息
}
