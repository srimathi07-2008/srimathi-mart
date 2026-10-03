package com.srimathi.srimathimart.service;

import com.srimathi.srimathimart.exception.DataAccessException;
import com.srimathi.srimathimart.util.Db;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Borrows a pooled connection, runs the supplied work, and commits or rolls
 * back. Keeping this in one place means no service has to repeat connection
 * and transaction bookkeeping, and no DAO ever opens its own connection.
 */
public final class TransactionTemplate {

    private TransactionTemplate() {
    }

    /** Work that runs inside a transaction and returns a value. */
    @FunctionalInterface
    public interface Work<T> {

        /**
         * Performs the unit of work.
         *
         * @param connection the transactional connection
         * @return the result
         */
        T run(Connection connection);
    }

    /** Work that runs inside a transaction and returns nothing. */
    @FunctionalInterface
    public interface VoidWork {

        /**
         * Performs the unit of work.
         *
         * @param connection the transactional connection
         */
        void run(Connection connection);
    }

    /**
     * Runs read-only work on an auto-commit connection.
     *
     * @param work the work to run
     * @param <T>  the result type
     * @return the result of the work
     */
    public static <T> T read(final Work<T> work) {
        try (Connection connection = Db.getConnection()) {
            return work.run(connection);
        } catch (SQLException ex) {
            throw new DataAccessException("Database read failed.", ex);
        }
    }

    /**
     * Runs work in an explicit transaction, committing on success and rolling
     * back on any exception.
     *
     * @param work the work to run
     * @param <T>  the result type
     * @return the result of the work
     */
    public static <T> T inTransaction(final Work<T> work) {
        Connection connection = null;
        boolean previousAutoCommit = true;

        try {
            connection = Db.getConnection();
            previousAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            T result = work.run(connection);

            connection.commit();
            return result;

        } catch (SQLException ex) {
            rollback(connection);
            throw new DataAccessException("Transaction failed.", ex);
        } catch (RuntimeException ex) {
            rollback(connection);
            throw ex;
        } finally {
            close(connection, previousAutoCommit);
        }
    }

    /**
     * Runs transactional work that returns nothing.
     *
     * @param work the work to run
     */
    public static void inTransaction(final VoidWork work) {
        inTransaction(connection -> {
            work.run(connection);
            return null;
        });
    }

    private static void rollback(final Connection connection) {
        if (connection == null) {
            return;
        }
        try {
            connection.rollback();
        } catch (SQLException ignored) {
            // The original failure is the one worth reporting.
        }
    }

    private static void close(final Connection connection, final boolean autoCommit) {
        if (connection == null) {
            return;
        }
        try {
            connection.setAutoCommit(autoCommit);
        } catch (SQLException ignored) {
            // Restoring the flag is best effort before returning to the pool.
        }
        try {
            connection.close();
        } catch (SQLException ignored) {
            // Closing a pooled connection just returns it to the pool.
        }
    }
}
