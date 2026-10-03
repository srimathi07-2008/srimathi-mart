package com.srimathi.srimathimart.service;

import com.srimathi.srimathimart.dao.JdbcUserDao;
import com.srimathi.srimathimart.dao.UserDao;
import com.srimathi.srimathimart.model.Role;
import com.srimathi.srimathimart.model.User;
import com.srimathi.srimathimart.util.Config;
import com.srimathi.srimathimart.util.PasswordUtil;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Creates the seed accounts at startup.
 *
 * <p>Why this exists rather than putting hashes in {@code seed.sql}: a bcrypt
 * hash committed to a public repository is a standing offer to anyone with a
 * GPU. Hashing at startup from a value in the untracked
 * {@code config.properties} means the repository never contains a credential,
 * and rotating the admin password is a config edit plus a restart.</p>
 *
 * <p>{@code seed.sql} still inserts the three account rows so the schema can be
 * populated standalone, but it leaves {@code password_hash} empty, which no
 * password can ever match. This class fills those blanks in on first boot.</p>
 *
 * <p>ADMIN is only ever created here. There is no HTTP path that can produce
 * an administrator account.</p>
 */
public final class SeedService {

    private static final Logger LOG = Logger.getLogger(SeedService.class.getName());

    private final UserDao userDao;

    /** Creates the seeder with the default JDBC DAO. */
    public SeedService() {
        this(new JdbcUserDao());
    }

    /**
     * Creates the seeder with a supplied DAO.
     *
     * @param userDaoValue the user DAO
     */
    public SeedService(final UserDao userDaoValue) {
        this.userDao = userDaoValue;
    }

    /**
     * Ensures the admin account exists with a usable password, and optionally
     * seeds the demo buyer and seller when {@code seed.demo.enabled} is true.
     */
    public void ensureSeedAccounts() {
        ensureAccount(
                Config.get("seed.admin.email", "admin@srimathimart.com"),
                Config.get("seed.admin.name", "Srimathi Admin"),
                Config.get("seed.admin.password", "ChangeMe@Admin1"),
                Role.ADMIN);

        if (!Boolean.parseBoolean(Config.get("seed.demo.enabled", "true"))) {
            return;
        }

        ensureAccount(
                Config.get("seed.seller.email", "seller@srimathimart.com"),
                Config.get("seed.seller.name", "Demo Seller"),
                Config.get("seed.seller.password", "ChangeMe@Seller1"),
                Role.SELLER);

        ensureAccount(
                Config.get("seed.buyer.email", "buyer@srimathimart.com"),
                Config.get("seed.buyer.name", "Demo Buyer"),
                Config.get("seed.buyer.password", "ChangeMe@Buyer1"),
                Role.BUYER);
    }

    private void ensureAccount(final String email,
                               final String fullName,
                               final String rawPassword,
                               final Role role) {

        TransactionTemplate.inTransaction(connection -> {
            String normalised = email.trim().toLowerCase(java.util.Locale.ROOT);
            Optional<User> existing = userDao.findByEmail(connection, normalised);

            if (existing.isPresent()) {
                User user = existing.get();

                // seed.sql inserts a blank hash; fill it in on first boot.
                if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()) {
                    userDao.updatePasswordHash(connection, user.getId(),
                            PasswordUtil.hash(rawPassword));
                    LOG.info(() -> "Seed account activated: " + normalised
                            + " (" + role + "). Change its password before deploying.");
                }
                return null;
            }

            User user = new User();
            user.setFullName(fullName);
            user.setEmail(normalised);
            user.setPasswordHash(PasswordUtil.hash(rawPassword));
            user.setRole(role);
            user.setActive(true);

            userDao.insert(connection, user);
            LOG.info(() -> "Seed account created: " + normalised
                    + " (" + role + "). Change its password before deploying.");
            return null;
        });
    }
}
