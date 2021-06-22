package com.service;

import java.sql.SQLException;

public interface RedisRedPacketService {
    //保存redis抢红包列表
    public void saveUserRedPacketByRedis(Long redPacketId, Double unitAmount) throws SQLException;
}
