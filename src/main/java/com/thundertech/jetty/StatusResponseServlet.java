package com.thundertech.jetty;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public class StatusResponseServlet extends HttpServlet {

    private AtomicLong requestsCount = new AtomicLong(0);

    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write("{ \"status\": \"ok\"}");
        System.out.println("Handled by: " + Thread.currentThread().getName());
        System.out.println("Requests count: " + requestsCount.addAndGet(1));
    }

}
