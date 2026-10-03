package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.model.Order;
import com.srimathi.srimathimart.model.OrderItem;
import com.srimathi.srimathimart.service.OrderService;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Turns the buyer's cart into an order.
 *
 * <p>Payment is mocked. No gateway is contacted, no card details are accepted,
 * and the payment reference in the response is a random string generated
 * locally purely so the confirmation screen has something to display.</p>
 */
@WebServlet(name = "checkoutServlet", urlPatterns = "/api/checkout")
public class CheckoutServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient OrderService orderService = new OrderService();

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            SessionUser buyer = requireUser(request);
            String paymentMethod = request.getParameter("paymentMethod");

if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
    paymentMethod = "COD";
}

Order order = orderService.placeOrderFromCart(
        buyer.getId(),
        paymentMethod
);

            Json json = new Json();
            json.beginObject()
                .put("ok", true)
                .put("message", "Payment confirmed (mock). Your order is placed.")
                .put("mockPayment", true)
                .name("order").beginObject()
                    .put("id", order.getId())
                    .put("status", order.getStatus())
                    .put("paymentRef", order.getPaymentRef())
                    .put("totalAmount", order.getTotalAmount())
                    .name("items").beginArray();

            for (OrderItem item : order.getItems()) {
                json.beginObject()
                    .put("productId", item.getProductId())
                    .put("name", item.getProductName())
                    .put("unitPrice", item.getUnitPrice())
                    .put("quantity", item.getQuantity())
                    .put("lineTotal", item.getLineTotal())
                    .endObject();
            }

            json.endArray().endObject().endObject();
            HttpUtil.writeJson(response, HttpServletResponse.SC_CREATED, json);
        });
    }
}
