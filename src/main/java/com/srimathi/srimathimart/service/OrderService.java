package com.srimathi.srimathimart.service;

import com.srimathi.srimathimart.dao.CartDao;
import com.srimathi.srimathimart.dao.JdbcCartDao;
import com.srimathi.srimathimart.dao.JdbcOrderDao;
import com.srimathi.srimathimart.dao.JdbcProductDao;
import com.srimathi.srimathimart.dao.OrderDao;
import com.srimathi.srimathimart.dao.ProductDao;
import com.srimathi.srimathimart.dto.CartLine;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.model.Order;
import com.srimathi.srimathimart.model.OrderItem;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;


public class OrderService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PAYMENT_REF_BYTES = 6;

    private final OrderDao orderDao;
    private final CartDao cartDao;
    private final ProductDao productDao;

    /** Creates the service with the default JDBC DAOs. */
    public OrderService() {
        this(new JdbcOrderDao(), new JdbcCartDao(), new JdbcProductDao());
    }

    /**
     * Creates the service with supplied DAOs.
     *
     * @param orderDaoValue   the order DAO
     * @param cartDaoValue    the cart DAO
     * @param productDaoValue the product DAO
     */
    public OrderService(final OrderDao orderDaoValue,
                        final CartDao cartDaoValue,
                        final ProductDao productDaoValue) {
        this.orderDao = orderDaoValue;
        this.cartDao = cartDaoValue;
        this.productDao = productDaoValue;
    }

    /**
     * Places an order from the buyer's current cart.
     *
     * @param buyerId the buyer
     * @return the placed order
     */
    public Order placeOrderFromCart(final long buyerId,final String paymentMethod) {
        return TransactionTemplate.inTransaction(connection -> {

            List<CartLine> lines = cartDao.findLines(connection, buyerId);
            if (lines.isEmpty()) {
                throw new ValidationException("Your cart is empty.");
            }

            Order order = new Order();
            order.setBuyerId(buyerId);
            order.setStatus("PLACED");
            order.setPaymentRef(mockPaymentReference());
            order.setPaymentMethod(paymentMethod);

            BigDecimal total = BigDecimal.ZERO;

            for (CartLine line : lines) {
                boolean deducted = productDao.decrementStock(
                        connection, line.getProductId(), line.getQuantity());

                if (!deducted) {
                    // Rolls the whole transaction back: no partial orders.
                    throw new ValidationException(
                            line.getName() + " just went out of stock. "
                            + "Adjust your cart and try again.");
                }

                OrderItem item = new OrderItem();
                item.setProductId(line.getProductId());
                item.setProductName(line.getName());
                item.setUnitPrice(line.getUnitPrice());
                item.setQuantity(line.getQuantity());
                order.addItem(item);

                total = total.add(line.getLineTotal());
            }

            order.setTotalAmount(total.setScale(2, RoundingMode.HALF_UP));

            Order saved = orderDao.insert(connection, order);
            cartDao.clear(connection, buyerId);
            return saved;
        });
    }

    /**
     * Lists a buyer's past orders.
     *
     * @param buyerId the buyer
     * @return the orders, newest first
     */
    public List<Order> listForBuyer(final long buyerId) {
        return TransactionTemplate.read(
                connection -> orderDao.findByBuyer(connection, buyerId));
    }
    public long countForSeller(final long sellerId) {
    return TransactionTemplate.read(
            connection -> orderDao.countBySeller(connection, sellerId));
}

public BigDecimal revenueForSeller(final long sellerId) {
    return TransactionTemplate.read(
            connection -> orderDao.revenueBySeller(connection, sellerId));
}

    /**
     * Generates a fake payment reference so the confirmation screen has
     * something to show. It identifies nothing and authorises nothing.
     *
     * @return a mock reference such as MOCK-3F2A91B4C7D0
     */
    private static String mockPaymentReference() {
        byte[] bytes = new byte[PAYMENT_REF_BYTES];
        RANDOM.nextBytes(bytes);

        StringBuilder builder = new StringBuilder("MOCK-");
        for (byte value : bytes) {
            builder.append(String.format(Locale.ROOT, "%02X", value));
        }
        return builder.toString();
    }
}
