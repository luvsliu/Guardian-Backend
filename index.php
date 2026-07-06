<?php 
// Permitir conexiones externas
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST, GET, OPTIONS");
header("Access-Control-Allow-Headers: Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With");

// Responder a las solicitudes preflight (OPTIONS)
if ($_SERVER['REQUEST_METHOD'] == 'OPTIONS') {
    http_response_code(200);
    exit();
}

// Configuración de conexión usando variables de entorno de Railway
$host = getenv('MYSQLHOST');
$user = getenv('MYSQLUSER');
$pass = getenv('MYSQLPASSWORD');
$db   = getenv('MYSQLDATABASE');
$port = getenv('MYSQLPORT');

// Crear la conexión
$conn = new mysqli($host, $user, $pass, $db, $port);

// Verificar la conexión
if ($conn->connect_error) {
    die(json_encode([
        "status" => "error", 
        "message" => "Error de conexión: " . $conn->connect_error
    ]));
}

// Leer el JSON que envía la aplicación Android
$json = file_get_contents('php://input');
$data = json_decode($json);

// Verificar que lleguen datos
if ($data && !empty($data->nombre) && !empty($data->numero)) {
    
    // Limpiar datos para evitar inyecciones SQL
    $nombre = $conn->real_escape_string($data->nombre);
    $numero = $conn->real_escape_string($data->numero);
    $parentesco = $conn->real_escape_string($data->parentesco);

    // Consulta SQL para insertar
    $sql = "INSERT INTO contactos (nombre, numero, parentesco) VALUES ('$nombre', '$numero', '$parentesco')";
    
    if ($conn->query($sql) === TRUE) {
        echo json_encode([
            "status" => "success", 
            "message" => "Contacto sincronizado con Guardian Lumina"
        ]);
    } else {
        echo json_encode([
            "status" => "error", 
            "message" => "Error al insertar: " . $conn->error
        ]);
    }
} else {
    // Si la petición es un GET simple (desde el navegador), mostrar un saludo
    if ($_SERVER['REQUEST_METHOD'] == 'GET') {
        echo json_encode([
            "status" => "online",
            "message" => "Servidor Guardian Lumina en Railway está activo"
        ]);
    } else {
        echo json_encode([
            "status" => "error", 
            "message" => "Datos incompletos o formato JSON inválido"
        ]);
    }
}

// Cerrar conexión
$conn->close();
?>
