package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.exception.AppException;
import com.srimathi.srimathimart.exception.AuthException;
import com.srimathi.srimathimart.exception.NotFoundException;
import com.srimathi.srimathimart.exception.ValidationException;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Shared plumbing for the servlets: one place that turns an exception into an
 * HTTP status and a safe message.
 *
 * <p>Controllers in this project contain no SQL and no business rules. They
 * read parameters, call a service, and serialise the result.</p>
 */
public abstract class BaseServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Logger available to subclasses. */
    protected static final Logger LOG = Logger.getLogger(BaseServlet.class.getName());

    /**
     * Runs an action, converting any application exception into a JSON error.
     *
     * @param request  the request
     * @param response the response
     * @param action   the work to perform
     * @throws IOException when writing the response fails
     */
    protected void handle(final HttpServletRequest request,
                          final HttpServletResponse response,
                          final Action action) throws IOException {
        try {
            action.run();

        } catch (ValidationException ex) {
            HttpUtil.writeError(response, HttpServletResponse.SC_BAD_REQUEST, ex.getMessage());

        } catch (AuthException ex) {
            HttpUtil.writeError(response, HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());

        } catch (NotFoundException ex) {
            HttpUtil.writeError(response, HttpServletResponse.SC_NOT_FOUND, ex.getMessage());

        } catch (AppException ex) {
            // Includes DataAccessException. The cause is logged for the
            // operator; the client is told nothing about the database.
            LOG.log(Level.SEVERE, "Request failed: " + request.getRequestURI(), ex);
            HttpUtil.writeError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Something went wrong. Please try again.");
        }
    }

    /**
     * Returns the signed-in user or throws.
     *
     * @param request the request
     * @return the session user
     */
    protected SessionUser requireUser(final HttpServletRequest request) {
        SessionUser user = HttpUtil.currentUser(request);
        if (user == null) {
            throw new AuthException("Please sign in to continue.");
        }
        return user;
    }

    /**
     * Writes a simple success envelope.
     *
     * @param response the response
     * @param message  the message
     * @throws IOException when writing fails
     */
    protected void writeOk(final HttpServletResponse response, final String message)
            throws IOException {
        Json json = new Json();
        json.beginObject().put("ok", true).put("message", message).endObject();
        HttpUtil.writeJson(response, HttpServletResponse.SC_OK, json);
    }

    /** A unit of controller work that may throw an application exception. */
    @FunctionalInterface
    protected interface Action {

        /**
         * Performs the work.
         *
         * @throws IOException when writing the response fails
         */
        void run() throws IOException;
    }
}
