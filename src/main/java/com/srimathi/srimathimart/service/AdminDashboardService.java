package com.srimathi.srimathimart.service;

import com.srimathi.srimathimart.dao.JdbcOrderDao;
import com.srimathi.srimathimart.dao.JdbcProductDao;
import com.srimathi.srimathimart.dao.JdbcUserDao;
import com.srimathi.srimathimart.dao.OrderDao;
import com.srimathi.srimathimart.dao.ProductDao;
import com.srimathi.srimathimart.dao.UserDao;
import java.math.BigDecimal;

/**
 * Provides real database statistics for the admin dashboard.
 */
public class AdminDashboardService {

    private final UserDao userDao;
    private final ProductDao productDao;
    private final OrderDao orderDao;

    public AdminDashboardService() {
        this(new JdbcUserDao(), new JdbcProductDao(), new JdbcOrderDao());
    }

    public AdminDashboardService(
            final UserDao userDao,
            final ProductDao productDao,
            final OrderDao orderDao) {

        this.userDao = userDao;
        this.productDao = productDao;
        this.orderDao = orderDao;
    }

    public DashboardStats getStats() {

        return TransactionTemplate.read(connection -> {

            long totalUsers =
                    userDao.countAll(connection);

            long totalSellers =
                    userDao.countByRole(connection, "SELLER");

            long totalProducts =
                    productDao.countAll(connection);

            long totalOrders =
                    orderDao.countAll(connection);

            BigDecimal totalRevenue =
                    orderDao.totalRevenue(connection);

            return new DashboardStats(
                    totalUsers,
                    totalSellers,
                    totalProducts,
                    totalOrders,
                    totalRevenue
            );
        });
    }

    public static final class DashboardStats {

        private final long totalUsers;
        private final long totalSellers;
        private final long totalProducts;
        private final long totalOrders;
        private final BigDecimal totalRevenue;

        public DashboardStats(
                final long totalUsers,
                final long totalSellers,
                final long totalProducts,
                final long totalOrders,
                final BigDecimal totalRevenue) {

            this.totalUsers = totalUsers;
            this.totalSellers = totalSellers;
            this.totalProducts = totalProducts;
            this.totalOrders = totalOrders;
            this.totalRevenue = totalRevenue;
        }

        public long getTotalUsers() {
            return totalUsers;
        }

        public long getTotalSellers() {
            return totalSellers;
        }

        public long getTotalProducts() {
            return totalProducts;
        }

        public long getTotalOrders() {
            return totalOrders;
        }

        public BigDecimal getTotalRevenue() {
            return totalRevenue;
        }
    }
}