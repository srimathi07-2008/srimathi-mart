package com.srimathi.srimathimart.util;

import com.srimathi.srimathimart.exception.AppException;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Single access point for the pooled {@link DataSource}.
 *
 * <p>The pool itself is created and destroyed by
 * {@code com.srimathi.srimathimart.listener.DataSourceListener}. No other class
 * in the application calls {@code DriverManager.getConnection()}.</p>
 */
public final class Db {

    private static volatile DataSource dataSource;

    private Db() {
    }

    /**
     * Installs the pooled data source. Called once by the context listener,
     * and by tests to swap in an H2 pool.
     *
     * @param value the data source to publish
     */
    public static void setDataSource(final DataSource value) {
        dataSource = value;
    }

    /**
     * Returns the active data source.
     *
     * @return the pooled data source
     */
    public static DataSource getDataSource() {
        DataSource current = dataSource;
        if (current == null) {
            throw new AppException("Data source is not initialised. "
                    + "DataSourceListener must run before any DAO call.");
        }
        return current;
    }

    /**
     * Borrows a connection from the pool. Callers must close it, ideally with
     * try-with-resources, which returns it to the pool rather than closing it.
     *
     * @return a pooled connection
     * @throws SQLException when the pool cannot hand one out
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}
