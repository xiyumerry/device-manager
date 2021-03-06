package com.sicau.devicemanager.service.impl;

import com.sicau.devicemanager.POJO.DO.UserAuth;
import com.sicau.devicemanager.config.RedisConfig;
import com.sicau.devicemanager.config.exception.CommonException;
import com.sicau.devicemanager.constants.CommonConstants;
import com.sicau.devicemanager.constants.HttpParamKey;
import com.sicau.devicemanager.constants.ResultEnum;
import com.sicau.devicemanager.dao.UserAuthMapper;
import com.sicau.devicemanager.dao.UserMapper;
import com.sicau.devicemanager.service.LoginService;
import com.sicau.devicemanager.util.DateUtil;
import com.sicau.devicemanager.util.JWTUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author BeFondOfTaro
 * Created at 21:10 2018/5/16
 */
@Service
public class LoginServiceImpl implements LoginService {

    /**
     * token过期时间(天)
     */
    @Value("${token-expire-time}")
    private int tokenExpireTime;

    @Autowired
    private UserAuthMapper userAuthMapper;
    @Autowired
	private RedisTemplate<String,String> redisTemplate;
    @Autowired
	private UserMapper userMapper;

    @Override
    public Map<String, Object> login(String identifier, String credential, Integer identifyType) {
        UserAuth userAuth = userAuthMapper.getUserAuthByLoginInfo(identifier, credential, identifyType);
        if (null == userAuth){
            throw new CommonException(ResultEnum.LOGIN_FAILED);
        }
        //根据userId，密码，token过期时间生成token
		String token = JWTUtil.sign(userAuth.getUserId(),
				credential,
				DateUtil.convertDay2Millisecond(tokenExpireTime));
        //存入redis
        redisTemplate.boundValueOps(CommonConstants.RedisKey.AUTH_TOKEN_PRIFIX + userAuth.getUserId()).
				set(token,tokenExpireTime, TimeUnit.DAYS);
        //返回给客户端
        Map<String,Object> res = new HashMap<>();
        res.put(HttpParamKey.TOKEN,token);
        res.put("userInfo",userMapper.getUserById(userAuth.getUserId()));
        return res;
    }

}
