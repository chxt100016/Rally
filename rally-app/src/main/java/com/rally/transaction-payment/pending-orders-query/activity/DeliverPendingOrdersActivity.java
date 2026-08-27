package com.rally.transactionpayment.pendingordersquery.activity;

import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PaymentOrderSummaryDTO;
import com.rally.domain.payment.service.PaymentQueryDomainService;
import com.rally.payment.convert.PaymentAppConvertMapper;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 deliver-pending-orders：交付当前付款人的全部本地待支付单。
 */
@Component
@RequiredArgsConstructor
public class DeliverPendingOrdersActivity {

    private final PaymentQueryDomainService paymentQueryDomainService;

    /**
     * 沿用 main 的查询与 DTO 转换链路，不引入额外筛选或渠道核实。
     *
     * <p>返回结果不分页、不限量且不承诺排序；仓储无命中时的空列表
     * 经现有 MapStruct 转换器继续交付为空列表。读取或枚举转换异常不捕获，
     * 保持原有入口的统一异常语义。</p>
     */
    public List<PaymentOrderSummaryDTO> execute() {
        // A1：当前登录用户是唯一付款人条件；现有领域服务仅查 PENDING。
        String payerUserId = UserContext.get();
        List<PaymentOrder> pendingOrders = paymentQueryDomainService.myPending(payerUserId);

        // A2-A3：复用既有 DTO，交付业务关联与三项金额；聚合映射 PENDING→UNPAID。
        return PaymentAppConvertMapper.INSTANCE.toSummaryList(pendingOrders);
    }
}
