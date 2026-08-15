package com.familykitchen.auth.service;

import com.familykitchen.common.error.BusinessException;
import com.familykitchen.common.error.ErrorCode;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/** 调用微信官方 code2Session 接口换取真实 OpenID。 */
@Component
public class WechatCodeExchangeClient {
  private final RestTemplate restTemplate = new RestTemplate();
  private final String appId;
  private final String appSecret;

  /**
   * 创建微信编码ExchangeClient实例。
   *
   * @param appId app标识
   * @param appSecret appSecret
   */
  public WechatCodeExchangeClient(@Value("#{environment.getProperty('family-kitchen.wechat.app-id','')}") String appId,
      @Value("#{environment.getProperty('family-kitchen.wechat.app-secret','')}") String appSecret) {
    this.appId=appId; this.appSecret=appSecret;
  }

  /**
   * 处理微信编码ExchangeClient。
   *
   * @param code 编码
   * @return 处理结果
   */
  public String exchange(String code) {
    if (appId.isBlank() || appSecret.isBlank()) {
      throw new BusinessException(ErrorCode.SYSTEM_ERROR,"微信真实环境配置缺失");
    }
    String url="https://api.weixin.qq.com/sns/jscode2session?appid={appid}&secret={secret}&js_code={code}&grant_type=authorization_code";
    Map<?,?> result=restTemplate.getForObject(url,Map.class,appId,appSecret,code);
    Object openId=result==null?null:result.get("openid");
    if (openId==null) {
      Object message=result==null?null:result.get("errmsg");
      throw new BusinessException(ErrorCode.BUSINESS_INVALID,"微信鉴权失败："+(message==null?"无有效响应":message));
    }
    return String.valueOf(openId);
  }
}
