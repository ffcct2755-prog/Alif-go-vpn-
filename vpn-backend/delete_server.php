<?php
// delete_server.php - Securely delete a server from the database

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

$input = json_decode(file_get_contents('php://input'), true);
if (!$input) {
    $input = $_POST;
}

$id = (int)($input['id'] ?? 0);

if ($id <= 0) {
    http_response_code(400);
    echo json_encode(["status" => "error", "message" => "Invalid server ID"]);
    exit();
}

$stmt = $conn->prepare("DELETE FROM vpn_servers WHERE id = ?");
$stmt->bind_param("i", $id);

if ($stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Server deleted successfully"]);
} else {
    http_response_code(500);
    echo json_encode(["status" => "error", "message" => "Failed to delete server: " . $stmt->error]);
}

$stmt->close();
$conn->close();
?>
