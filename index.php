<?php 
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Variables de Railway
$host = getenv('MYSQLHOST');
$user = getenv('MYSQLUSER');
$pass = getenv('MYSQLPASSWORD');
$db   = getenv('MYSQLDATABASE');
$port = getenv('MYSQLPORT');

// Intentar conexión
$conn = new mysqli($host, $user, $pass, $db, $port);

if ($conn->connect_error) {
    http_response_code(500); // Error de servidor
    echo json_encode(["status" => "error", "message" => "DB Connection Failed: " . $conn->connect_error]);
    exit();
}

$json = file_get_contents('php://input');
$data = json_decode($json);

if ($data && !empty($data->nombre) && !empty($data->numero)) {
    $nombre = $conn->real_escape_string($data->nombre);
    $numero = $conn->real_escape_string($data->numero);
    $parentesco = $conn->real_escape_string($data->parentesco);

    $sql = "INSERT INTO contactos (nombre, numero, parentesco) VALUES ('$nombre', '$numero', '$parentesco')";
    
    if ($conn->query($sql) === TRUE) {
        http_response_code(201); // Creado con éxito
        echo json_encode(["status" => "success", "message" => "Contacto guardado"]);
    } else {
        http_response_code(400); // Error en la petición
        echo json_encode(["status" => "error", "message" => "SQL Error: " . $conn->error]);
    }
} else {
    if ($_SERVER['REQUEST_METHOD'] == 'GET') {
        echo json_encode(["status" => "online", "message" => "Ready"]);
    } else {
        http_response_code(400);
        echo json_encode(["status" => "error", "message" => "Invalid JSON or missing fields"]);
    }
}
$conn->close();
?>
