package com.anjia.unidbgserver.web;

import com.alibaba.fastjson.JSONObject;
import com.anjia.unidbgserver.service.AoBiShieldWorker;
import com.anjia.unidbgserver.service.MoleShieldWorker;
import javax.annotation.Resource;


import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping(path = {"/api"}, produces = {"application/json"})
public class ResponseController {
    @Resource(name = "MoleShieldWorker")
    private MoleShieldWorker MoleShieldWorker;

    @Resource(name = "AoBiShieldWorker")
    private AoBiShieldWorker AoBiShieldWorker;

    @SneakyThrows
    @RequestMapping(value = {"/getSafeComm"}, method = {RequestMethod.POST}, produces = {"application/json;charset=UTF-8"})
    public String getMoleShieldParamsNew(@RequestBody JSONObject jsonObject) {
        String GameType = jsonObject.getString("GameType");
        if (GameType != null && GameType.equals("aobi")) {
            String Encrypt = AoBiShieldWorker.AoBiShield(jsonObject).get();
            JSONObject result = new JSONObject();
            result.put("Encrypt",  Encrypt);
            result.put("GameType",  GameType);
            return result.toJSONString();
        }
        String Encrypt = MoleShieldWorker.MoleShield(jsonObject).get();
        JSONObject result = new JSONObject();
        result.put("Encrypt",  Encrypt);
        result.put("GameType",  "mole");
        return result.toJSONString();
    }

}
