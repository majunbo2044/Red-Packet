package com.service;
//保存抢红包信息
public interface UserRedPacketService {
    public int grapRedPacket(Long redPacketId, Long userId);
    public Long grapRedPacketByRedis(Long redPacketId,Long userId);
}
