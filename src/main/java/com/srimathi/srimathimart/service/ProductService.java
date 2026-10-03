package com.srimathi.srimathimart.service;

import com.srimathi.srimathimart.dao.JdbcProductDao;
import com.srimathi.srimathimart.dao.ProductDao;
import com.srimathi.srimathimart.dto.ProductForm;
import com.srimathi.srimathimart.dto.ProductSearch;
import com.srimathi.srimathimart.exception.AuthException;
import com.srimathi.srimathimart.exception.NotFoundException;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.model.Product;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Catalogue rules: validation, ownership checks, and the buyer-facing search.
 *
 * <p>Ownership is enforced here rather than in the DAO. Every seller mutation
 * reloads the product and compares its {@code seller_id} with the caller, so a
 * seller cannot edit or delete another seller's listing by guessing an id.</p>
 */
public class ProductService {

    private static final int MAX_NAME_LENGTH = 160;
    private static final int MAX_DESCRIPTION_LENGTH = 1000;
    private static final int MAX_CATEGORY_LENGTH = 60;
    private static final int MAX_IMAGE_URL_LENGTH = 500;
    private static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");

    private final ProductDao productDao;

    /** Creates the service with the default JDBC DAO. */
    public ProductService() {
        this(new JdbcProductDao());
    }

    /**
     * Creates the service with a supplied DAO.
     *
     * @param productDaoValue the product DAO
     */
    public ProductService(final ProductDao productDaoValue) {
        this.productDao = productDaoValue;
    }

    /**
     * Creates a listing owned by the given seller.
     *
     * @param sellerId the owning seller
     * @param form     the submitted fields
     * @return the saved product
     */
    public Product create(final long sellerId, final ProductForm form) {
    Product product = validate(form, new Product());
    product.setSellerId(sellerId);
    product.setActive(true);

    TransactionTemplate.Work<Product> work =
            connection -> productDao.insert(connection, product);

    return TransactionTemplate.inTransaction(work);
}

    /**
     * Updates a listing the seller owns.
     *
     * @param sellerId  the caller
     * @param productId the listing to update
     * @param form      the submitted fields
     * @return the updated product
     */
    public Product update(final long sellerId, final long productId, final ProductForm form) {
        return TransactionTemplate.inTransaction(connection -> {
            Product existing = productDao.findById(connection, productId)
                    .orElseThrow(() -> new NotFoundException("That product no longer exists."));

            requireOwner(existing, sellerId);

            Product updated = validate(form, existing);
            updated.setId(productId);
            updated.setSellerId(existing.getSellerId());

            productDao.update(connection, updated);
            return updated;
        });
    }

    /**
     * Deletes a listing the seller owns.
     *
     * @param sellerId  the caller
     * @param productId the listing to delete
     */
    public void delete(final long sellerId, final long productId) {
        TransactionTemplate.inTransaction(connection -> {
            Product existing = productDao.findById(connection, productId)
                    .orElseThrow(() -> new NotFoundException("That product no longer exists."));

            requireOwner(existing, sellerId);
            productDao.delete(connection, productId);
        });
    }

    /**
     * Lists one seller's own products.
     *
     * @param sellerId the seller
     * @return the seller's listings
     */
    public List<Product> listForSeller(final long sellerId) {
        return TransactionTemplate.read(
                connection -> productDao.findBySeller(connection, sellerId));
    }

    /**
     * Browses the public catalogue with optional keyword and category filters.
     *
     * @param search the filter
     * @return matching products
     */
    public List<Product> browse(final ProductSearch search) {
        return TransactionTemplate.read(connection -> productDao.search(connection, search));
    }

    /**
     * Loads one product.
     *
     * @param productId the product id
     * @return the product
     */
    public Product get(final long productId) {
        return TransactionTemplate.read(connection -> productDao.findById(connection, productId)
                .orElseThrow(() -> new NotFoundException("That product no longer exists.")));
    }

    /**
     * Lists the categories currently in use.
     *
     * @return sorted category names
     */
    public List<String> categories() {
        return TransactionTemplate.read(productDao::findCategories);
    }

    private static void requireOwner(final Product product, final long sellerId) {
        if (product.getSellerId() != sellerId) {
            throw new AuthException("You can only manage your own products.");
        }
    }

    private static Product validate(final ProductForm form, final Product target) {
        String name = text(form.getName(), "Product name", MAX_NAME_LENGTH, true);
        String category = text(form.getCategory(), "Category", MAX_CATEGORY_LENGTH, true);
        String description =
                text(form.getDescription(), "Description", MAX_DESCRIPTION_LENGTH, false);
        String imageUrl = text(form.getImageUrl(), "Image URL", MAX_IMAGE_URL_LENGTH, false);

        BigDecimal price = parsePrice(form.getPrice());
        int stock = parseStock(form.getStockQuantity());

        target.setName(name);
        target.setCategory(category);
        target.setDescription(description);
        target.setImageUrl(imageUrl);
        target.setPrice(price);
        target.setStockQuantity(stock);
        return target;
    }

    private static String text(final String raw,
                               final String label,
                               final int maxLength,
                               final boolean required) {
        String value = raw == null ? "" : raw.trim();
        if (required && value.isEmpty()) {
            throw new ValidationException(label + " is required.");
        }
        if (value.length() > maxLength) {
            throw new ValidationException(label + " must be " + maxLength + " characters or fewer.");
        }
        return value;
    }

    private static BigDecimal parsePrice(final String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException("Price is required.");
        }

        // Tolerate the currency symbols and separators the existing UI shows,
        // so a seller can paste "Rs 1,499" or "1499.00" and both work.
        String cleaned = raw.trim().replaceAll("[^0-9.\\-]", "");

        BigDecimal price;
        try {
            price = new BigDecimal(cleaned);
        } catch (NumberFormatException ex) {
            throw new ValidationException("Enter a valid price.");
        }

        if (price.signum() < 0) {
            throw new ValidationException("Price cannot be negative.");
        }
        if (price.compareTo(MAX_PRICE) > 0) {
            throw new ValidationException("Price is too large.");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private static int parseStock(final String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        int stock;
        try {
            stock = Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            throw new ValidationException("Enter a valid stock quantity.");
        }
        if (stock < 0) {
            throw new ValidationException("Stock quantity cannot be negative.");
        }
        return stock;
    }
}
