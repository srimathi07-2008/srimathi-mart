package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.LoginRequest;
import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.model.Role;
import com.srimathi.srimathimart.service.AuthService;
import com.srimathi.srimathimart.util.Config;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Handles all three sign-in forms. The page posts a {@code role} field so the
 * seller page cannot be used to sign a buyer in, and the response tells the
 * page where to go next.
 */
@WebServlet(name = "loginServlet", urlPatterns = "/api/login")
public class LoginServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    /** Default idle timeout in seconds when nothing is configured. */
    private static final int DEFAULT_SESSION_TIMEOUT_SECONDS = 1800;

    private final transient AuthService authService = new AuthService();

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            LoginRequest form = new LoginRequest(
                    HttpUtil.param(request, "email"),
                    request.getParameter("password"),
                    Role.from(HttpUtil.param(request, "role")));

            SessionUser user = authService.login(form);

            // Session fixation defence: drop whatever session id the visitor
            // arrived with and issue a new one now that they are privileged.
            HttpSession existing = request.getSession(false);
            if (existing != null) {
                existing.invalidate();
            }

            HttpSession session = request.getSession(true);
            session.setAttribute(HttpUtil.SESSION_USER, user);
            session.setMaxInactiveInterval(
                    Config.getInt("session.timeout.seconds", DEFAULT_SESSION_TIMEOUT_SECONDS));

            Json json = new Json();
            json.beginObject()
                .put("ok", true)
                .put("message", "Signed in.")
                .name("user").beginObject()
                    .put("id", user.getId())
                    .put("fullName", user.getFullName())
                    .put("email", user.getEmail())
                    .put("role", user.getRole().name())
                .endObject()
                .put("redirect", landingPageFor(user.getRole()))
                .endObject();

            HttpUtil.writeJson(response, HttpServletResponse.SC_OK, json);
        });
    }

    /**
     * The page a role lands on after signing in. These are exactly the pages
     * the original front end navigated to, so the flow is unchanged.
     *
     * @param role the signed-in role
     * @return a relative page name
     */
    static String landingPageFor(final Role role) {
        switch (role) {
            case SELLER:
                return "seller-dashboard.html";
            case ADMIN:
                return "admin-dashboard.html";
            case BUYER:
            default:
                return "home.html";
        }
    }
}
