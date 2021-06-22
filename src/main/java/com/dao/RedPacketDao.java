package com.dao;

import com.entity.RedPacket;
import org.springframework.stereotype.Repository;

@Repository
public interface RedPacketDao {
    public RedPacket getRedPacket(Long id);//获取红包信息
    public int decreaseRedPacket(Long id);//扣减抢红包数
}
