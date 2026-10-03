package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.RegisterRequest;
import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.model.Role;
import com.srimathi.srimathimart.service.AuthService;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Buyer and seller registration.
 *
 * <p>The role comes from a hidden field on the signup form, but the service
 * rejects anything other than BUYER or SELLER, so posting {@code role=ADMIN}
 * to this endpoint cannot create an administrator.</p>
 */
@WebServlet(name = "registerServlet", urlPatterns = "/api/register")
public class RegisterServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    private final transient AuthService authService = new AuthService();

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            RegisterRequest form = new RegisterRequest(
                    HttpUtil.param(request, "fullName"),
                    HttpUtil.param(request, "email"),
                    request.getParameter("password"),
                    request.getParameter("confirmPassword"),
                    Role.from(HttpUtil.param(request, "role")));

            SessionUser user = authService.register(form);

            // Sign the new account straight in, with a fresh session id.
            HttpSession session = request.getSession(true);
            session.invalidate();

            HttpSession fresh = request.getSession(true);
            fresh.setAttribute(HttpUtil.SESSION_USER, user);

            Json json = new Json();
            json.beginObject()
                .put("ok", true)
                .put("message", "Welcome to Srimathi Mart, " + user.getFullName() + ".")
                .name("user").beginObject()
                    .put("id", user.getId())
                    .put("fullName", user.getFullName())
                    .put("email", user.getEmail())
                    .put("role", user.getRole().name())
                .endObject()
                .put("redirect", LoginServlet.landingPageFor(user.getRole()))
                .endObject();

            HttpUtil.writeJson(response, HttpServletResponse.SC_CREATED, json);
        });
    }
}
