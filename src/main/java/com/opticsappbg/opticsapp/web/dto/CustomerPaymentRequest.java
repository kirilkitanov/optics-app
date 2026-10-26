
package com.opticsappbg.opticsapp.web.dto;

import com.opticsappbg.opticsapp.customer.model.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CustomerPaymentRequest {

    private BigDecimal amount;
    private PaymentMethod paymentMethod;
}
