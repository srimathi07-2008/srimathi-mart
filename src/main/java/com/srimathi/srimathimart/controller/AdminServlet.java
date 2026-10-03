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

@WebServlet("/api/admin/*")
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

            String path = request.getPathInfo();

            if ("/users".equals(path)) {
                loadUsers(response);
            } else {
                loadDashboard(response);
            }
        });
    }

    private void loadDashboard(HttpServletResponse response)
            throws IOException {

        try (Connection connection =
                     Db.getDataSource().getConnection()) {

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

        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Could not load admin dashboard.",
                    ex
            );
        }
    }

    private void loadUsers(HttpServletResponse response)
            throws IOException {

        String sql =
                "SELECT id, full_name, email, role, active, created_at "
                + "FROM users ORDER BY id DESC";

        try (Connection connection =
                     Db.getDataSource().getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            Json json = new Json()
                    .beginObject()
                    .put("ok", true)
                    .name("users")
                    .beginArray();

            while (rs.next()) {

                json.beginObject()
                        .put("id", rs.getLong("id"))
                        .put("fullName", rs.getString("full_name"))
                        .put("email", rs.getString("email"))
                        .put("role", rs.getString("role"))
                        .put("joined", rs.getTimestamp("created_at") == null
                                ? ""
                                : rs.getTimestamp("created_at").toString())
                        .put("active", rs.getBoolean("active"))
                        .endObject();
            }

            json.endArray()
                    .endObject();

            HttpUtil.writeJson(
                    response,
                    HttpServletResponse.SC_OK,
                    json
            );

        } catch (SQLException ex) {
            throw new RuntimeException(
                    "Could not load users.",
                    ex
            );
        }
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