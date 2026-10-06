package com.srimathi.srimathimart.controller;

import com.srimathi.srimathimart.util.Config;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@WebServlet("/api/chat")
public class ChatbotServlet extends HttpServlet {

    private static final String API_URL =
            "https://api.openai.com/v1/responses";

    private final HttpClient client = HttpClient.newHttpClient();

    @Override
    protected void doPost(HttpServletRequest request,
                           HttpServletResponse response) throws IOException {

        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String message = request.getParameter("message");

        if (message == null || message.trim().isEmpty()) {
            response.getWriter().write(
                    "{\"reply\":\"Please enter a message.\"}"
            );
            return;
        }

        String apiKey = Config.get("openai.apiKey", "");
        String model = Config.get("openai.model", "gpt-6-luna");

        if (apiKey.isBlank()) {
            response.setStatus(500);
            response.getWriter().write(
                    "{\"reply\":\"AI service is not configured.\"}"
            );
            return;
        }

        String instructions =
                "You are the helpful AI assistant for Srimathi Mart. " +
                "Srimathi Mart sells home decoration items and wedding/event flower decorations. " +
                "Answer clearly and briefly. " +
                "You can explain products, decorations, cart, orders and general website usage. " +
                "Do not claim that you can see private customer data or actual order details. " +
                "If the user asks about their personal order, tell them to check the Orders section.";

        String json =
                "{"
                + "\"model\":\"" + escapeJson(model) + "\","
                + "\"instructions\":\"" + escapeJson(instructions) + "\","
                + "\"input\":\"" + escapeJson(message) + "\""
                + "}";

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        try {
            HttpResponse<String> apiResponse =
                    client.send(
                            httpRequest,
                            HttpResponse.BodyHandlers.ofString()
                    );

            if (apiResponse.statusCode() >= 200 &&
                apiResponse.statusCode() < 300) {

                String reply = extractReply(apiResponse.body());

                response.getWriter().write(
                        "{\"reply\":\"" + escapeJson(reply) + "\"}"
                );

            } else {
                response.setStatus(500);
                response.getWriter().write(
                        "{\"reply\":\"Sorry, AI service is temporarily unavailable.\"}"
                );
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            response.setStatus(500);
            response.getWriter().write(
                    "{\"reply\":\"Sorry, something went wrong.\"}"
            );
        }
    }

    private String extractReply(String json) {

        Pattern pattern = Pattern.compile(
                "\"text\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\""
        );

        Matcher matcher = pattern.matcher(json);

        if (matcher.find()) {
            return unescapeJson(matcher.group(1));
        }

        return "Sorry, I could not understand the AI response.";
    }

    private String escapeJson(String text) {

        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String text) {

        return text
                .replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }
}