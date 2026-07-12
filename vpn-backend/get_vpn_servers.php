<?php
// get_vpn_servers.php - Returns list of active VPN servers (GET) or updates them (POST)
// Query URL for Android App: https://yourdomain.com/get_vpn_servers.php

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type, X-API-Key, Authorization');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    exit(0);
}

// Database connection setup
if (file_exists('db_connect.php')) {
    require_once 'db_connect.php';
} else {
    $host = "sql113.infinityfree.com";
    $username = "if0_42306300";
    $password = "GGLn5I9D5iQLGu3";
    $dbname = "if0_42306300_alif_go_vpn";

    $conn = new mysqli($host, $username, $password, $dbname);

    if ($conn->connect_error) {
        http_response_code(500);
        echo json_encode([
            "status" => "error",
            "message" => "Database connection failed"
        ]);
        exit();
    }
    $conn->set_charset("utf8mb4");
}

$method = $_SERVER['REQUEST_METHOD'];

if ($method === 'POST') {
    // 1. Receive and Sync server list from the Android App
    $jsonInput = file_get_contents('php://input');
    $servers = json_decode($jsonInput, true);

    if ($servers === null) {
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "Invalid JSON payload"]);
        $conn->close();
        exit();
    }

    // Begin Transaction to prevent partial updates
    $conn->begin_transaction();

    try {
        // Clear all existing servers
        $conn->query("DELETE FROM vpn_servers");

        if (!empty($servers)) {
            // Prepare dynamic bulk insert
            $stmt = $conn->prepare("INSERT INTO vpn_servers (country_name, country_code, city, ip_address, type, latency, load_percent, protocol) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            
            foreach ($servers as $srv) {
                $countryName = $srv['countryName'] ?? '';
                $countryCode = $srv['countryCode'] ?? '';
                $city = $srv['city'] ?? '';
                $ipAddress = $srv['ipAddress'] ?? '';
                $type = $srv['type'] ?? 'Free';
                $latency = (int)($srv['latency'] ?? 50);
                $loadPercent = (int)($srv['loadPercent'] ?? 30);
                $protocol = $srv['protocol'] ?? 'WireGuard';

                if (!empty($countryName) && !empty($ipAddress)) {
                    $stmt->bind_param("sssssiis", $countryName, $countryCode, $city, $ipAddress, $type, $latency, $loadPercent, $protocol);
                    $stmt->execute();
                }
            }
            $stmt->close();
        }

        $conn->commit();
        echo json_encode(["status" => "success", "message" => "Successfully synced " . count($servers) . " servers with database"]);

    } catch (Exception $e) {
        $conn->rollback();
        http_response_code(500);
        echo json_encode(["status" => "error", "message" => "Failed to sync: " . $e->getMessage()]);
    }

} else {
    // 2. Return current servers (GET method)
    $sql = "SELECT id, country_name, country_code, city, ip_address, type, latency, load_percent, is_enabled, protocol FROM vpn_servers WHERE is_enabled = 1";
    $result = $conn->query($sql);

    $servers = [];

    if ($result && $result->num_rows > 0) {
        while($row = $result->fetch_assoc()) {
            $servers[] = [
                "id" => (int)$row["id"],
                "countryName" => $row["country_name"],
                "countryCode" => $row["country_code"],
                "city" => $row["city"],
                "ipAddress" => $row["ip_address"],
                "type" => $row["type"],
                "latency" => (int)$row["latency"],
                "loadPercent" => (int)$row["load_percent"],
                "isEnabled" => (bool)$row["is_enabled"],
                "protocol" => $row["protocol"]
            ];
        }
    }

    echo json_encode($servers, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES);
}

$conn->close();
?>
