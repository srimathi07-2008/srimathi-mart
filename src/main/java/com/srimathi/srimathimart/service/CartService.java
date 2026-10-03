package com.srimathi.srimathimart.service;

import com.srimathi.srimathimart.dao.CartDao;
import com.srimathi.srimathimart.dao.JdbcCartDao;
import com.srimathi.srimathimart.dao.JdbcProductDao;
import com.srimathi.srimathimart.dao.ProductDao;
import com.srimathi.srimathimart.dto.CartLine;
import com.srimathi.srimathimart.dto.CartView;
import com.srimathi.srimathimart.exception.NotFoundException;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.model.Product;
import java.util.List;

/**
 * Cart rules.
 *
 * <p>The cart lives in the database rather than in localStorage, so it
 * survives a logout and cannot be edited by the browser. Quantities are
 * checked against live stock on every mutation.</p>
 */
public class CartService {

    /** Upper bound per line, to stop a typo becoming a 10,000 unit order. */
    private static final int MAX_QUANTITY_PER_LINE = 99;

    private final CartDao cartDao;
    private final ProductDao productDao;

    /** Creates the service with the default JDBC DAOs. */
    public CartService() {
        this(new JdbcCartDao(), new JdbcProductDao());
    }

    /**
     * Creates the service with supplied DAOs.
     *
     * @param cartDaoValue    the cart DAO
     * @param productDaoValue the product DAO
     */
    public CartService(final CartDao cartDaoValue, final ProductDao productDaoValue) {
        this.cartDao = cartDaoValue;
        this.productDao = productDaoValue;
    }

    /**
     * Adds a product to a buyer's cart.
     *
     * @param buyerId   the buyer
     * @param productId the product
     * @param quantity  how many to add
     * @return the refreshed cart
     */
    public CartView add(final long buyerId, final long productId, final int quantity) {
        if (quantity < 1) {
            throw new ValidationException("Quantity must be at least 1.");
        }

        return TransactionTemplate.inTransaction(connection -> {
            Product product = productDao.findById(connection, productId)
                    .orElseThrow(() -> new NotFoundException("That product is no longer listed."));

            if (!product.isActive()) {
                throw new ValidationException("That product is no longer available.");
            }

            int alreadyInCart = currentQuantity(cartDao.findLines(connection, buyerId), productId);
            int requested = alreadyInCart + quantity;

            guardQuantity(requested, product);

            cartDao.addOrIncrement(connection, buyerId, productId, quantity);
            return new CartView(cartDao.findLines(connection, buyerId));
        });
    }

    /**
     * Sets an exact quantity for one line, removing it when set to zero.
     *
     * @param buyerId   the buyer
     * @param productId the product
     * @param quantity  the new quantity
     * @return the refreshed cart
     */
    public CartView updateQuantity(final long buyerId, final long productId, final int quantity) {
        if (quantity < 0) {
            throw new ValidationException("Quantity cannot be negative.");
        }

        return TransactionTemplate.inTransaction(connection -> {
            if (quantity == 0) {
                cartDao.remove(connection, buyerId, productId);
                return new CartView(cartDao.findLines(connection, buyerId));
            }

            Product product = productDao.findById(connection, productId)
                    .orElseThrow(() -> new NotFoundException("That product is no longer listed."));

            guardQuantity(quantity, product);

            boolean updated = cartDao.updateQuantity(connection, buyerId, productId, quantity);
            if (!updated) {
                cartDao.addOrIncrement(connection, buyerId, productId, quantity);
            }

            return new CartView(cartDao.findLines(connection, buyerId));
        });
    }

    /**
     * Removes a line from the cart.
     *
     * @param buyerId   the buyer
     * @param productId the product
     * @return the refreshed cart
     */
    public CartView remove(final long buyerId, final long productId) {
        return TransactionTemplate.inTransaction(connection -> {
            cartDao.remove(connection, buyerId, productId);
            return new CartView(cartDao.findLines(connection, buyerId));
        });
    }

    /**
     * Loads a buyer's cart with its running total.
     *
     * @param buyerId the buyer
     * @return the cart view
     */
    public CartView view(final long buyerId) {
        return TransactionTemplate.read(
                connection -> new CartView(cartDao.findLines(connection, buyerId)));
    }

    /**
     * Empties a buyer's cart.
     *
     * @param buyerId the buyer
     */
    public void clear(final long buyerId) {
        // Written as a block lambda on purpose: an expression lambda here is
        // ambiguous between Work<Integer> and VoidWork and will not compile.
        TransactionTemplate.inTransaction(connection -> {
            cartDao.clear(connection, buyerId);
        });
    }

    private static int currentQuantity(final List<CartLine> lines, final long productId) {
        for (CartLine line : lines) {
            if (line.getProductId() == productId) {
                return line.getQuantity();
            }
        }
        return 0;
    }

    private static void guardQuantity(final int requested, final Product product) {
        if (requested > MAX_QUANTITY_PER_LINE) {
            throw new ValidationException(
                    "You can order at most " + MAX_QUANTITY_PER_LINE + " of one item.");
        }
        if (requested > product.getStockQuantity()) {
            throw new ValidationException(
                    "Only " + product.getStockQuantity() + " left in stock for "
                    + product.getName() + ".");
        }
    }
}
