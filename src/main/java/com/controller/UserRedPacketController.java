package com.controller;

import com.service.RedPacketService;
import com.service.UserRedPacketService;
import com.service.serviceImpl.RedPacketServiceImpl;
import com.service.serviceImpl.UserRedPacketServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/userRedPacket")
public class UserRedPacketController {
    @Autowired
    private UserRedPacketService userRedPacketService = null;
    @RequestMapping("/grapRedPacket")
    @ResponseBody//转换为json返回给前端请求
    public Map<String,Object> grapRedPacket(Long redPacketId,Long userId){
        Map<String,Object> retMap = new HashMap<String, Object>();
        int result = userRedPacketService.grapRedPacket(redPacketId,userId);
//        Long result = userRedPacketService.grapRedPacketByRedis(redPacketId,userId);
        boolean flag =result>0;
        retMap.put("success",flag);
        retMap.put("message",flag?"抢红包成功":"抢红包失败");
        return retMap;
    }
}
