package id.holigo.services.common.model.pulsa;

import java.math.BigDecimal;
import java.sql.Timestamp;

import id.holigo.services.common.model.OrderStatusEnum;
import id.holigo.services.common.model.PaymentStatusEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrepaidPulsaTransactionDto {
    private Integer id;

    private Timestamp createdAt;

    private Timestamp updatedAt;

    private Timestamp deletedAt;

    private String serialNumber;

    private String supplierTransactionId;

    private String supplierServiceCode;

    private String supplierProductCode;

    private String supplierNominalCode;

    private Integer serviceId;

    private Integer productId;

    private Integer nominalId;

    private String customerNumber;

    private BigDecimal fareAmount;

    private BigDecimal discountAmount;

    private BigDecimal markupAmount;

    private BigDecimal ntaAmount;

    private BigDecimal nraAmount;

    private String device;

    private PaymentStatusEnum paymentStatus;

    private OrderStatusEnum orderStatus;

    private Integer onCheck;
    
}
