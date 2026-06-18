import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * 黑客松題目 A：智慧健康日誌與風險評估系統 — 後端主程式
 * 使用 Java 內建 HttpServer，前後端分離架構
 */
public class HealthServer {

    private static final String DB_URL =
            "jdbc:mysql://localhost:3306/hackathon_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Taipei";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "123456";
    private static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        System.out.println("=== HealthServer 啟動中 ===");

        // 啟動前先測試 MySQL 連線
        if (!testDatabaseConnection()) {
            System.err.println("[失敗] 無法連線 MySQL，程式結束。");
            System.err.println("請確認：");
            System.err.println("  1. MySQL 服務已啟動");
            System.err.println("  2. 資料庫 hackathon_db 已建立");
            System.err.println("  3. 帳號 root / 密碼 123456 正確");
            System.exit(1);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // 健康檢查 API（確認 Server 有在跑）
        server.createContext("/api/health", exchange -> {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                sendJson(exchange, 200, "{\"status\":\"ok\",\"message\":\"Server is running\"}");
            } else {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        });

        // 資料庫連線測試 API
        server.createContext("/api/db-test", exchange -> {
            addCorsHeaders(exchange);
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }
            if ("GET".equals(exchange.getRequestMethod())) {
                try (Connection conn = getConnection()) {
                    sendJson(exchange, 200, "{\"status\":\"ok\",\"database\":\"connected\"}");
                } catch (SQLException e) {
                    sendJson(exchange, 500,
                            "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
                }
            } else {
                sendJson(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        });

        server.setExecutor(null);
        server.start();

        System.out.println("[成功] HealthServer 已啟動");
        System.out.println("  網址: http://localhost:" + PORT);
        System.out.println("  測試: http://localhost:" + PORT + "/api/health");
        System.out.println("  資料庫測試: http://localhost:" + PORT + "/api/db-test");
        System.out.println("按 Ctrl+C 可停止 Server");
    }

    private static boolean testDatabaseConnection() {
        System.out.println("正在測試 MySQL 連線...");
        System.out.println("  主機: localhost:3306");
        System.out.println("  資料庫: hackathon_db");

        try (Connection conn = getConnection()) {
            System.out.println("[成功] MySQL 連線成功！");
            return true;
        } catch (SQLException e) {
            System.err.println("[失敗] MySQL 連線失敗: " + e.getMessage());
            return false;
        }
    }

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
    }

    private static void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
