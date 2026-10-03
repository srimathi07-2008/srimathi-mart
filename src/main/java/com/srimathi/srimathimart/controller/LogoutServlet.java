package com.srimathi.srimathimart.controller;

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/** Ends the session. Accepts GET so the existing "Logout" links keep working. */
@WebServlet(name = "logoutServlet", urlPatterns = "/api/logout")
public class LogoutServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        endSession(request, response);
    }

    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        endSession(request, response);
    }

    private void endSession(final HttpServletRequest request,
                            final HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        if (com.srimathi.srimathimart.util.HttpUtil.wantsJson(request)) {
            writeOk(response, "Signed out.");
        } else {
            response.sendRedirect(request.getContextPath() + "/index.html");
        }
    }
}
