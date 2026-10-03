package com.srimathi.srimathimart.filter;

import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.model.Role;
import com.srimathi.srimathimart.util.HttpUtil;
import java.io.IOException;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Guards protected paths.
 *
 * <p>Two behaviours, chosen by what the caller asked for. A browser navigating
 * to a protected page is redirected to the matching sign-in page. A fetch()
 * call gets a 401 with a JSON body, so the existing scripts can show a message
 * instead of accidentally rendering a login page inside a product grid.</p>
 *
 * <p>The filter is intentionally a deny-list of protected prefixes rather than
 * an allow-list, because the public catalogue and the static assets are the
 * common case and must stay reachable to a signed-out visitor exactly as they
 * are today.</p>
 *
 * <p>Registered in {@code web.xml} rather than with {@code @WebFilter} so the
 * ordering between the two filters is explicit: encoding must run first.</p>
 */
public class AuthFilter implements Filter {

    /** Protected path prefix, mapped to the role it demands. */
    private static final Map<String, Role> PROTECTED_PREFIXES = Map.ofEntries(
            Map.entry("/api/cart", Role.BUYER),
            Map.entry("/api/checkout", Role.BUYER),
            Map.entry("/api/orders", Role.BUYER),
            Map.entry("/api/seller", Role.SELLER),
            Map.entry("/seller-dashboard.html", Role.SELLER),
            Map.entry("/seller.html", Role.SELLER),
            Map.entry("/cart.html", Role.BUYER),
            Map.entry("/checkout.html", Role.BUYER),
            Map.entry("/order-confirmation.html", Role.BUYER),
            Map.entry("/admin-dashboard.html", Role.ADMIN),
            Map.entry("/api/admin", Role.ADMIN));

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // Nothing to configure.
    }

    @Override
    public void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse,
                         final FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String path = request.getRequestURI()
                .substring(request.getContextPath().length());

        Role required = requiredRole(path);

        if (required == null) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        SessionUser user = HttpUtil.currentUser(request);

        if (user == null) {
            deny(request, response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Please sign in to continue.", loginPageFor(required));
            return;
        }

        // An admin can reach anything; otherwise the role must match exactly.
        if (user.getRole() != required && user.getRole() != Role.ADMIN) {
            deny(request, response, HttpServletResponse.SC_FORBIDDEN,
                    "Your account does not have access to that area.",
                    loginPageFor(required));
            return;
        }

        chain.doFilter(servletRequest, servletResponse);
    }

    private static Role requiredRole(final String path) {
        for (Map.Entry<String, Role> entry : PROTECTED_PREFIXES.entrySet()) {
            if (path.equals(entry.getKey()) || path.startsWith(entry.getKey() + "/")) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static String loginPageFor(final Role role) {
        switch (role) {
            case SELLER:
                return "seller-login.html";
            case ADMIN:
                return "admin-login.html";
            case BUYER:
            default:
                return "login.html";
        }
    }

    private static void deny(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final int status,
                             final String message,
                             final String loginPage) throws IOException {

        if (HttpUtil.wantsJson(request)) {
            HttpUtil.writeError(response, status, message);
            return;
        }

        response.sendRedirect(request.getContextPath() + "/" + loginPage);
    }

    @Override
    public void destroy() {
        // Nothing to release.
    }
}
