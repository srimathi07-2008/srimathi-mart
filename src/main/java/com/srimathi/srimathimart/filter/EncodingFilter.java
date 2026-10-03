package com.srimathi.srimathimart.filter;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class EncodingFilter implements Filter {

    private static final String ENCODING = "UTF-8";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Nothing to configure
    }

    @Override
    public void doFilter(
            ServletRequest request,
                                final
            ServletResponse response,
                                final
            FilterChain chain)
            throws IOException, ServletException {

        // Force UTF-8 for incoming request
        request.setCharacterEncoding("UTF-8");

        // Force UTF-8 for outgoing response
        response.setCharacterEncoding("UTF-8");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {
        // Nothing to release
    }
}