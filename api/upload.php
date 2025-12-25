<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

$response = array();

// Get folder from POST or default to 'general'
$folder = $_POST['folder'] ?? 'general';
$uploadDir = __DIR__ . '/images/' . $folder . '/';

// Create directory if it doesn't exist
if (!file_exists($uploadDir)) {
    if (!mkdir($uploadDir, 0755, true)) {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Failed to create upload directory'
        ]);
        exit();
    }
}

// Handle base64 encoded image (from Android app)
if (isset($_POST['image'])) {
    $image = $_POST['image']; // Base64 encoded string
    
    // Remove base64 prefix if present (e.g., "data:image/png;base64,")
    if (strpos($image, 'data:image') === 0) {
        $image = explode(',', $image)[1];
    }
    
    // Decode base64 string
    $decodedImage = base64_decode($image, true);
    
    // Check if decoding succeeded
    if ($decodedImage === false) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid base64 image data'
        ]);
        exit();
    }
    
    // Validate image size (max 5MB)
    $maxSize = 5 * 1024 * 1024; // 5MB in bytes
    if (strlen($decodedImage) > $maxSize) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'File size exceeds maximum limit of 5MB'
        ]);
        exit();
    }
    
    // Detect image type from base64 or use default
    $imageType = 'png';
    if (isset($_POST['imageType'])) {
        $imageType = $_POST['imageType'];
    } elseif (isset($_POST['type'])) {
        $imageType = $_POST['type'];
    }
    
    // Generate unique filename
    $imageName = uniqid() . '_' . time() . '.' . $imageType;
    $uploadPath = $uploadDir . $imageName;
    
    // Save image to server
    if (file_put_contents($uploadPath, $decodedImage)) {
        $relativePath = 'api/images/' . $folder . '/' . $imageName;
        
        // Get base URL - construct proper URL for image access
        $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https' : 'http';
        $host = $_SERVER['HTTP_HOST'];
        
        // Get the base path more reliably
        $scriptName = $_SERVER['SCRIPT_NAME']; // e.g., /skeetskeet/api/upload.php
        $scriptDir = dirname($scriptName); // e.g., /skeetskeet/api
        $baseDir = dirname($scriptDir); // e.g., /skeetskeet
        
        // Construct full URL
        $url = $protocol . '://' . $host . $baseDir . '/api/images/' . $folder . '/' . $imageName;
        
        // Log for debugging (remove in production)
        error_log("Image uploaded: $uploadPath, URL: $url");
        
        echo json_encode([
            'success' => true,
            'message' => 'Image uploaded successfully',
            'path' => $relativePath,
            'url' => $url,
            'imageName' => $imageName
        ]);
    } else {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Failed to save image file'
        ]);
    }
}
// Handle multipart file upload (from web forms)
elseif (isset($_FILES['image'])) {
    $file = $_FILES['image'];
    
    // Validate file type
    $allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
    $fileType = $file['type'];
    
    if (!in_array($fileType, $allowedTypes)) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'Invalid file type. Only JPEG, PNG, GIF, and WebP are allowed.'
        ]);
        exit();
    }
    
    // Validate file size (max 5MB)
    $maxSize = 5 * 1024 * 1024; // 5MB in bytes
    if ($file['size'] > $maxSize) {
        http_response_code(400);
        echo json_encode([
            'success' => false,
            'message' => 'File size exceeds maximum limit of 5MB'
        ]);
        exit();
    }
    
    // Generate unique filename
    $fileName = time() . '_' . uniqid() . '_' . basename($file['name']);
    $targetPath = $uploadDir . $fileName;
    
    if (move_uploaded_file($file['tmp_name'], $targetPath)) {
        $relativePath = 'api/images/' . $folder . '/' . $fileName;
        
        // Get base URL - construct proper URL for image access
        $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? 'https' : 'http';
        $host = $_SERVER['HTTP_HOST'];
        
        // Get the base path more reliably
        $scriptName = $_SERVER['SCRIPT_NAME']; // e.g., /skeetskeet/api/upload.php
        $scriptDir = dirname($scriptName); // e.g., /skeetskeet/api
        $baseDir = dirname($scriptDir); // e.g., /skeetskeet
        
        // Construct full URL
        $url = $protocol . '://' . $host . $baseDir . '/api/images/' . $folder . '/' . $fileName;
        
        // Log for debugging (remove in production)
        error_log("Image uploaded: $targetPath, URL: $url");
        
        echo json_encode([
            'success' => true,
            'message' => 'Image uploaded successfully',
            'path' => $relativePath,
            'url' => $url,
            'imageName' => $fileName
        ]);
    } else {
        http_response_code(500);
        echo json_encode([
            'success' => false,
            'message' => 'Failed to upload image'
        ]);
    }
} else {
    http_response_code(400);
    echo json_encode([
        'success' => false,
        'message' => 'No image provided. Send either base64 encoded image in POST["image"] or multipart file in $_FILES["image"]'
    ]);
}
?>
