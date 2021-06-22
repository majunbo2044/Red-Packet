package com.service;

import com.entity.RedPacket;
//获取红包
public interface RedPacketService {
    public RedPacket getRedPacket(Long id);
    public int decreaseRedPacket(Long id);
}
