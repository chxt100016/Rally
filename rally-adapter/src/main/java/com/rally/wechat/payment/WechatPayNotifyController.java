package com.rally.wechat.payment;

import com.rally.payment.PaymentAppService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 微信支付异步回调（设计 §5.2）。
 * <p>
 * 接口路径：{@code /api/rally/wechat/pay/notify}（与 wechat.pay.pay-notify-url 对齐）。
 * 验签解密、状态推进、分账触发全部下沉 {@link PaymentAppService#handlePayCallback}；
 * 这里只负责把 body + headers 转给 app 层，并按微信要求写应答体。
 */
@Slf4j
@RestController
@RequestMapping("/wechat/pay")
@RequiredArgsConstructor
public class WechatPayNotifyController {

    private final PaymentAppService paymentAppService;

    /**
     * 支付异步回调
     */
    @PostMapping({"/notify", "/share-notify"})
    public Map<String, String> notify(@RequestBody String body, HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> headers = readHeaders(request);
        boolean success = paymentAppService.handlePayCallback(body, headers);
        Map<String, String> resp = new HashMap<>(2);
        if (success) {
            resp.put("code", "SUCCESS");
            resp.put("message", "成功");
        } else {
            // 非 200 状态码会让微信重试
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.put("code", "FAIL");
            resp.put("message", "处理失败");
        }
        return resp;
    }

    private Map<String, String> readHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>();
        java.util.Enumeration<String> names = request.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers;
    }
}
