package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.model.Order;
import com.srimathi.srimathimart.service.OrderService;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;
import java.io.IOException;
import java.util.List;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/** Lists the signed-in buyer's order history. */
@WebServlet(name = "orderServlet", urlPatterns = "/api/orders")
public class OrderServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient OrderService orderService = new OrderService();

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            SessionUser buyer = requireUser(request);
            List<Order> orders = orderService.listForBuyer(buyer.getId());

            Json json = new Json();
            json.beginObject()
                .put("ok", true)
                .put("count", orders.size())
                .name("orders").beginArray();

            for (Order order : orders) {
                json.beginObject()
                    .put("id", order.getId())
                    .put("status", order.getStatus())
                    .put("paymentRef", order.getPaymentRef())
                    .put("totalAmount", order.getTotalAmount())
                    .put("createdAt",
                            order.getCreatedAt() == null ? "" : order.getCreatedAt().toString())
                    .endObject();
            }

            json.endArray().endObject();
            HttpUtil.writeJson(response, HttpServletResponse.SC_OK, json);
        });
    }
}
