<?php
// db_connect.php - Connect to MySQL Database securely

$host = "sql113.infinityfree.com";
$username = "if0_42306300";
$password = "GGLn5I9D5iQLGu3";
$dbname = "if0_42306300_alif_go_vpn";

// Create connection
$conn = new mysqli($host, $username, $password, $dbname);

// Check connection
if ($conn->connect_error) {
    header('Content-Type: application/json');
    http_response_code(500);
    echo json_encode([
        "status" => "error",
        "message" => "Database connection failed: " . $conn->connect_error
    ]);
    exit();
}

// Set charset to utf8mb4
$conn->set_charset("utf8mb4");
?>
