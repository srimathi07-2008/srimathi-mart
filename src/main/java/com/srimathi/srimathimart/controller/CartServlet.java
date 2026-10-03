package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.CartLine;
import com.srimathi.srimathimart.dto.CartView;
import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.service.CartService;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Cart operations for the signed-in buyer.
 *
 * <p>The buyer id always comes from the session, never from the request, so a
 * buyer cannot read or modify another buyer's cart by changing a parameter.</p>
 */
@WebServlet(name = "cartServlet", urlPatterns = "/api/cart")
public class CartServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient CartService cartService = new CartService();

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            SessionUser buyer = requireUser(request);
            writeCart(response, HttpServletResponse.SC_OK, null,
                    cartService.view(buyer.getId()));
        });
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            SessionUser buyer = requireUser(request);

            String action = HttpUtil.param(request, "action").toLowerCase();
            long productId = HttpUtil.longParam(request, "productId", -1L);

            if (productId <= 0) {
                throw new ValidationException("A product id is required.");
            }

            final CartView cart;
            final String message;

            switch (action) {
                case "":
                case "add":
                    cart = cartService.add(buyer.getId(), productId,
                            HttpUtil.intParam(request, "quantity", 1));
                    message = "Added to your cart.";
                    break;
                case "update":
                    cart = cartService.updateQuantity(buyer.getId(), productId,
                            HttpUtil.intParam(request, "quantity", 1));
                    message = "Cart updated.";
                    break;
                case "remove":
                    cart = cartService.remove(buyer.getId(), productId);
                    message = "Removed from your cart.";
                    break;
                default:
                    throw new ValidationException("Unknown action: " + action);
            }

            writeCart(response, HttpServletResponse.SC_OK, message, cart);
        });
    }

    private void writeCart(final HttpServletResponse response,
                           final int status,
                           final String message,
                           final CartView cart) throws IOException {

        Json json = new Json();
        json.beginObject().put("ok", true);

        if (message != null) {
            json.put("message", message);
        }

        json.put("distinctCount", cart.getDistinctCount())
            .put("totalQuantity", cart.getTotalQuantity())
            .put("subtotal", cart.getSubtotal())
            .put("total", cart.getSubtotal())
            .name("items").beginArray();

        for (CartLine line : cart.getLines()) {
            json.beginObject()
                .put("productId", line.getProductId())
                .put("name", line.getName())
                .put("imageUrl", line.getImageUrl())
                .put("unitPrice", line.getUnitPrice())
                .put("quantity", line.getQuantity())
                .put("stockQuantity", line.getStockQuantity())
                .put("lineTotal", line.getLineTotal())
                .endObject();
        }

        json.endArray().endObject();
        HttpUtil.writeJson(response, status, json);
    }
}
