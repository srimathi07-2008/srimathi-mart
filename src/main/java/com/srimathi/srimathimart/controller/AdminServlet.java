package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.exception.AuthException;
import com.srimathi.srimathimart.util.Db;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/api/admin/dashboard")
public class AdminServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {

        handle(request, response, () -> {

            SessionUser user = requireUser(request);

            if (user.getRole() == null
                    || !"ADMIN".equals(user.getRole().name())) {
                throw new AuthException("Admin access required.");
            }

            try {
                Connection connection = Db.getDataSource().getConnection();

                try {
                    long users = count(
                            connection,
                            "SELECT COUNT(*) FROM users"
                    );

                    long sellers = count(
                            connection,
                            "SELECT COUNT(*) FROM users WHERE role='SELLER'"
                    );

                    long products = count(
                            connection,
                            "SELECT COUNT(*) FROM products"
                    );

                    long orders = count(
                            connection,
                            "SELECT COUNT(*) FROM orders"
                    );

                    Json json = new Json()
                            .beginObject()
                            .put("ok", true)
                            .put("users", users)
                            .put("sellers", sellers)
                            .put("products", products)
                            .put("orders", orders)
                            .endObject();

                    HttpUtil.writeJson(
                            response,
                            HttpServletResponse.SC_OK,
                            json
                    );

                } finally {
                    connection.close();
                }

            } catch (SQLException ex) {
                throw new RuntimeException(
                        "Could not load admin dashboard.",
                        ex
                );
            }
        });
    }

    private long count(Connection connection, String sql)
            throws SQLException {

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            if (rs.next()) {
                return rs.getLong(1);
            }

            return 0;
        }
    }
}