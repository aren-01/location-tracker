<?php
header("Content-Type: application/json");

// Change the cridentials below!
$host = "localhost";
$user = "root";
$pass = "";
$dbname = "";


$conn = new mysqli($host, $user, $pass, $dbname);
if ($conn->connect_error) {
    die(json_encode(["status" => "error", "message" => "Connection failed: " . $conn->connect_error]));
}


if (!isset($_GET['x']) || !isset($_GET['y']) || !isset($_GET['key'])) {
    http_response_code(400);
    echo json_encode([
        "status" => "error",
        "message" => "Missing 'x', 'y', or 'key' parameters"
    ]);
    exit();
}

$x = floatval($_GET['x']);
$y = floatval($_GET['y']);
$key = $_GET['key'];


$stmt = $conn->prepare("INSERT INTO locations (x, y, user_key) VALUES (?, ?, ?)");
$stmt->bind_param("dds", $x, $y, $key);

if ($stmt->execute()) {
    echo json_encode(["status" => "success", "message" => "Location saved with key"]);
} else {
    echo json_encode(["status" => "error", "message" => "Insert failed: " . $stmt->error]);
}

$stmt->close();
$conn->close();
?>
