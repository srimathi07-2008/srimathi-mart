package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.dto.SessionUser;
import com.srimathi.srimathimart.util.HttpUtil;
import com.srimathi.srimathimart.util.Json;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Tells the page who is signed in. The existing navigation bar uses this to
 * decide whether the profile button shows a name or a sign-in link, without
 * any page needing to be converted to a JSP.
 */
@WebServlet(name = "sessionServlet", urlPatterns = "/api/session")
public class SessionServlet extends BaseServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {

        handle(request, response, () -> {
            SessionUser user = HttpUtil.currentUser(request);
            Json json = new Json();

            json.beginObject().put("ok", true);

            if (user == null) {
                json.put("authenticated", false);
            } else {
                json.put("authenticated", true)
                    .name("user").beginObject()
                        .put("id", user.getId())
                        .put("fullName", user.getFullName())
                        .put("email", user.getEmail())
                        .put("role", user.getRole().name())
                    .endObject();
            }

            json.endObject();
            HttpUtil.writeJson(response, HttpServletResponse.SC_OK, json);
        });
    }
}
