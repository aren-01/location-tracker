<?php
$servername = "localhost";
$username = "root";
$password = ""; 
$dbname = ""; 
// change the cridentials above if you are using any other DB
$conn = new mysqli($servername, $username, $password, $dbname);


if ($conn->connect_error) {
    die("Connection failed: " . $conn->connect_error);
}


if (!isset($_GET['key'])) {
    echo "no_key";
    exit();
}

$key = $_GET['key'];


$stmt = $conn->prepare("SELECT * FROM users WHERE user_key = ?");
if (!$stmt) {
    die("SQL error: " . $conn->error);  
}

$stmt->bind_param("s", $key);
$stmt->execute();
$result = $stmt->get_result();

if ($result && $result->num_rows > 0) {
    echo "valid";
} else {
    echo "invalid";
}

$stmt->close();
$conn->close();
?>
