<?php
// add_server.php - Securely add a new server to the backend database
// Accessible from the web management dashboard

header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

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

// Check if request is POST
if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(["status" => "error", "message" => "Method not allowed. Use POST."]);
    exit();
}

// Get JSON or Form input
$input = json_decode(file_get_contents('php://input'), true);
if (!$input) {
    $input = $_POST;
}

$country_name = trim($input['countryName'] ?? $input['country_name'] ?? '');
$country_code = trim($input['countryCode'] ?? $input['country_code'] ?? '');
$city         = trim($input['city'] ?? '');
$ip_address   = trim($input['ipAddress'] ?? $input['ip_address'] ?? '');
$type         = trim($input['type'] ?? 'Free');
$latency      = (int)($input['latency'] ?? rand(15, 80));
$load_percent = (int)($input['loadPercent'] ?? $input['load_percent'] ?? rand(5, 50));
$protocol     = trim($input['protocol'] ?? 'WireGuard');

if (empty($country_name) || empty($ip_address)) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Missing required fields (countryName, ipAddress)"]);
    exit();
}

if (empty($country_code)) {
    $country_code = "SG";
}

// Prepare SQL statement to avoid SQL Injection
$stmt = $conn->prepare("INSERT INTO vpn_servers (country_name, country_code, city, ip_address, type, latency, load_percent, protocol) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
$stmt->bind_param("sssssiis", $country_name, $country_code, $city, $ip_address, $type, $latency, $load_percent, $protocol);

if ($stmt->execute()) {
    echo json_encode([
        "status" => "success",
        "message" => "Server added successfully",
        "serverId" => $stmt->insert_id
    ]);
} else {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Failed to save server: " . $stmt->error]);
}

$stmt->close();
$conn->close();
?>
