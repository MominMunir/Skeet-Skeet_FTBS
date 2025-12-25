<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');

$path = $_GET['path'] ?? '';

if (empty($path)) {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'Path parameter is required'
    ]);
    exit();
}

$fullPath = __DIR__ . '/../' . $path;

if (file_exists($fullPath) && is_file($fullPath)) {
    // Get base URL
    $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https' : 'http';
    $host = $_SERVER['HTTP_HOST'];
    $basePath = dirname(dirname($_SERVER['SCRIPT_NAME']));
    $url = $protocol . '://' . $host . $basePath . '/' . $path;
    
    echo json_encode([
        'success' => true,
        'url' => $url,
        'path' => $path
    ]);
} else {
    http_response_code(404);
    echo json_encode([
        'success' => false,
        'message' => 'Image not found'
    ]);
}
?>
