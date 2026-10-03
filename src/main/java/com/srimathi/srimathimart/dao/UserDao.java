package com.srimathi.srimathimart.dao;

import com.srimathi.srimathimart.model.User;
import java.sql.Connection;
import java.util.Optional;
import java.util.List;
/**
 * Persistence for {@code users}.
 *
 * <p>DAOs do plain storage only: no validation, no hashing, no authorisation.
 * Those rules live in the service layer.</p>
 */
public interface UserDao {

    /**
     * Inserts a user and returns it with the generated id populated.
     *
     * @param connection an open connection
     * @param user       the user to insert, password hash already computed
     * @return the inserted user
     */
    User insert(Connection connection, User user);

    /**
     * Finds a user by email. Email comparison is case-insensitive because the
     * service layer lower-cases before storing.
     *
     * @param connection an open connection
     * @param email      the email to look up
     * @return the user, if present
     */
    Optional<User> findByEmail(Connection connection, String email);

    /**
     * Finds a user by id.
     *
     * @param connection an open connection
     * @param id         the user id
     * @return the user, if present
     */
    Optional<User> findById(Connection connection, long id);

    /**
     * Tests whether an email is already taken.
     *
     * @param connection an open connection
     * @param email      the email to check
     * @return true when a row already exists
     */
    boolean existsByEmail(Connection connection, String email);

    /**
     * Replaces a user's password hash.
     *
     * @param connection   an open connection
     * @param userId       the user id
     * @param passwordHash the new bcrypt hash
     * @return true when a row was updated
     */
    boolean updatePasswordHash(Connection connection, long userId, String passwordHash);

    /**
     * Counts users holding a role.
     *
     * @param connection an open connection
     * @param role       the role name
     * @return the number of matching rows
     */
    long countAll(Connection connection);
    long countByRole(Connection connection, String role);
    List<User> findRecentUsers(Connection connection);
}
