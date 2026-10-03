package com.srimathi.srimathimart.listener;

import com.srimathi.srimathimart.service.SeedService;
import com.srimathi.srimathimart.util.Config;
import com.srimathi.srimathimart.util.Db;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Owns the HikariCP pool for the whole application.
 *
 * <p>This is the only place a pool is created and the only place it is shut
 * down. Nothing else in the codebase calls
 * {@code DriverManager.getConnection()}; every DAO receives a connection that
 * came from this pool by way of
 * {@code com.srimathi.srimathimart.util.Db}.</p>
 *
 * <p>Credentials are read through {@code Config}, which prefers system
 * properties and environment variables over the untracked
 * {@code config.properties}. No password is compiled into this class.</p>
 */
@WebListener
public class DataSourceListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(DataSourceListener.class.getName());

    private static final int DEFAULT_POOL_SIZE = 10;
    private static final int DEFAULT_MIN_IDLE = 2;
    private static final long DEFAULT_CONNECTION_TIMEOUT_MS = 30_000L;
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 600_000L;
    private static final long DEFAULT_MAX_LIFETIME_MS = 1_740_000L;

    private HikariDataSource dataSource;

    @Override
    public void contextInitialized(final ServletContextEvent event) {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(Config.require("db.url"));
        config.setUsername(Config.require("db.username"));
        config.setPassword(Config.require("db.password"));

        String driver = Config.get("db.driver", "com.mysql.cj.jdbc.Driver");
        if (!driver.isBlank()) {
            config.setDriverClassName(driver);
        }

        config.setPoolName("srimathi-mart-pool");
        config.setMaximumPoolSize(Config.getInt("db.pool.maxSize", DEFAULT_POOL_SIZE));
        config.setMinimumIdle(Config.getInt("db.pool.minIdle", DEFAULT_MIN_IDLE));
        config.setConnectionTimeout(DEFAULT_CONNECTION_TIMEOUT_MS);
        config.setIdleTimeout(DEFAULT_IDLE_TIMEOUT_MS);
        config.setMaxLifetime(DEFAULT_MAX_LIFETIME_MS);
        config.setAutoCommit(true);

        // Server-side prepared statement caching. These are MySQL specific and
        // are simply ignored by other drivers.
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");

        dataSource = new HikariDataSource(config);
        Db.setDataSource(dataSource);

        event.getServletContext().setAttribute("dataSource", dataSource);

        LOG.info("Srimathi Mart: HikariCP pool started.");

        // Seeding runs here, not in its own @WebListener, because the servlet
        // spec does not guarantee an ordering between annotated listeners and
        // the seeder must not run before the pool exists.
        try {
            new SeedService().ensureSeedAccounts();
        } catch (RuntimeException ex) {
            LOG.log(Level.WARNING,
                    "Seed accounts could not be created. Has schema.sql been run?", ex);
        }
    }

    @Override
    public void contextDestroyed(final ServletContextEvent event) {
        if (dataSource != null && !dataSource.isClosed()) {
            try {
                dataSource.close();
                LOG.info("Srimathi Mart: HikariCP pool closed.");
            } catch (RuntimeException ex) {
                LOG.log(Level.WARNING, "Failed to close the connection pool cleanly.", ex);
            }
        }
        Db.setDataSource(null);
    }
}
