package com.hng.walletService.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Slf4j
@Controller
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String home() {
        log.info("Serving home page");

        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>HNG Wallet Service</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <style>
        body {
            font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
            margin: 0;
            padding: 0;
            background: #020617;
            color: #e5e7eb;
        }
        .hero {
            min-height: 100vh;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
            padding: 2rem;
            text-align: center;
        }
        .tag {
            display: inline-block;
            padding: 0.15rem 0.6rem;
            margin-bottom: 0.75rem;
            font-size: 0.75rem;
            border-radius: 999px;
            background: rgba(148, 163, 184, 0.25);
        }
        h1 {
            font-size: 2.4rem;
            margin-bottom: 0.5rem;
        }
        p.subtitle {
            max-width: 520px;
            margin: 0 auto 1.5rem;
            color: #9ca3af;
        }
        .actions {
            display: flex;
            flex-wrap: wrap;
            gap: 0.75rem;
            justify-content: center;
            margin-top: 0.75rem;
        }
        a.button {
            padding: 0.6rem 1.2rem;
            border-radius: 999px;
            text-decoration: none;
            font-size: 0.9rem;
            border: 1px solid rgba(148, 163, 184, 0.6);
        }
        a.button.primary {
            background: #22c55e;
            color: #022c22;
            border-color: #22c55e;
        }
        a.button.secondary {
            color: #e5e7eb;
        }
        .meta {
            margin-top: 2rem;
            font-size: 0.8rem;
            color: #6b7280;
        }
        code {
            background: rgba(15, 23, 42, 0.9);
            padding: 0.12rem 0.35rem;
            border-radius: 0.25rem;
            font-size: 0.8rem;
        }
    </style>
</head>
<body>
<div class="hero">
    <div class="tag">HNG Wallet API · Paystack · Google OAuth</div>

    <h1>HNG Wallet Service</h1>

    <p class="subtitle">
        A wallet API with deposits, transfers, transactions, API keys and Paystack integration.
    </p>

    <div class="actions">
        <a href="/swagger-ui/index.html" class="button primary">
            View API Documentation
        </a>
    </div>
</div>
</body>
</html>
                """;
    }
}