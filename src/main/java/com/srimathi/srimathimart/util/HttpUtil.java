package com.srimathi.srimathimart.util;

import com.srimathi.srimathimart.dto.SessionUser;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Small helpers shared by the servlets. */
public final class HttpUtil {

    /** Session attribute holding the logged-in {@link SessionUser}. */
    public static final String SESSION_USER = "sessionUser";

    private HttpUtil() {
    }

    /**
     * Reads a trimmed request parameter.
     *
     * @param request the request
     * @param name    the parameter name
     * @return the trimmed value, or an empty string when absent
     */
    public static String param(final HttpServletRequest request, final String name) {
        String value = request.getParameter(name);
        return value == null ? "" : value.trim();
    }

    /**
     * Reads a long request parameter.
     *
     * @param request      the request
     * @param name         the parameter name
     * @param defaultValue value used when absent or unparseable
     * @return the parsed value
     */
    public static long longParam(final HttpServletRequest request,
                                 final String name,
                                 final long defaultValue) {
        try {
            return Long.parseLong(param(request, name));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Reads an int request parameter.
     *
     * @param request      the request
     * @param name         the parameter name
     * @param defaultValue value used when absent or unparseable
     * @return the parsed value
     */
    public static int intParam(final HttpServletRequest request,
                               final String name,
                               final int defaultValue) {
        try {
            return Integer.parseInt(param(request, name));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Returns the logged in user, or null when the session is anonymous.
     *
     * @param request the request
     * @return the session user or null
     */
    public static SessionUser currentUser(final HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_USER);
        return value instanceof SessionUser ? (SessionUser) value : null;
    }

    /**
     * True when the caller expects JSON rather than a redirect. The existing
     * front end sends fetch() calls with X-Requested-With, plain form posts
     * do not.
     *
     * @param request the request
     * @return true when a JSON response is wanted
     */
    public static boolean wantsJson(final HttpServletRequest request) {
        String requestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(requestedWith)) {
            return true;
        }
        String accept = request.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }

    /**
     * Writes a JSON body with the given status.
     *
     * @param response the response
     * @param status   the HTTP status
     * @param body     the JSON text
     * @throws IOException when the socket write fails
     */
    public static void writeJson(final HttpServletResponse response,
                                 final int status,
                                 final String body) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        try (PrintWriter writer = response.getWriter()) {
            writer.write(body);
        }
    }

    /**
     * Writes a {ok:true,...} style JSON envelope.
     *
     * @param response the response
     * @param status   the HTTP status
     * @param json     an already-built JSON object writer
     * @throws IOException when the socket write fails
     */
    public static void writeJson(final HttpServletResponse response,
                                 final int status,
                                 final Json json) throws IOException {
        writeJson(response, status, json.toString());
    }

    /**
     * Writes a standard error envelope.
     *
     * @param response the response
     * @param status   the HTTP status
     * @param message  a safe, user-facing message
     * @throws IOException when the socket write fails
     */
    public static void writeError(final HttpServletResponse response,
                                  final int status,
                                  final String message) throws IOException {
        Json json = new Json();
        json.beginObject()
            .put("ok", false)
            .put("error", message)
            .endObject();
        writeJson(response, status, json);
    }
}
