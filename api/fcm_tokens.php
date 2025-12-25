<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once 'con.php';

$method = $_SERVER['REQUEST_METHOD'];

switch ($method) {
    case 'POST':
        // Register FCM token
        $data = json_decode(file_get_contents('php://input'), true);
        
        // Validate required fields
        if (empty($data['userId']) || empty($data['token'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields: userId, token']);
            break;
        }
        
        $userId = $data['userId'];
        $token = $data['token'];
        $platform = $data['platform'] ?? 'android';
        
        // Check if token already exists for this user
        $stmt = $pdo->prepare("SELECT * FROM fcm_tokens WHERE userId = ? AND token = ?");
        $stmt->execute([$userId, $token]);
        $existing = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if ($existing) {
            // Update last used timestamp
            $stmt = $pdo->prepare("UPDATE fcm_tokens SET lastUsed = ? WHERE userId = ? AND token = ?");
            $stmt->execute([time() * 1000, $userId, $token]);
            echo json_encode(['success' => true, 'message' => 'Token updated']);
        } else {
            // Insert new token
            $stmt = $pdo->prepare("INSERT INTO fcm_tokens (userId, token, platform, createdAt, lastUsed) VALUES (?, ?, ?, ?, ?)");
            $stmt->execute([
                $userId,
                $token,
                $platform,
                time() * 1000,
                time() * 1000
            ]);
            echo json_encode(['success' => true, 'message' => 'Token registered']);
        }
        break;
        
    case 'GET':
        if (isset($_GET['userId'])) {
            // Get tokens for a user
            $userId = $_GET['userId'];
            $stmt = $pdo->prepare("SELECT * FROM fcm_tokens WHERE userId = ?");
            $stmt->execute([$userId]);
            $tokens = $stmt->fetchAll(PDO::FETCH_ASSOC);
            echo json_encode($tokens);
        } else {
            // Get all tokens
            $stmt = $pdo->query("SELECT * FROM fcm_tokens");
            $tokens = $stmt->fetchAll(PDO::FETCH_ASSOC);
            echo json_encode($tokens);
        }
        break;
        
    case 'DELETE':
        // Delete token (on logout)
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (empty($data['userId']) || empty($data['token'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields: userId, token']);
            break;
        }
        
        $stmt = $pdo->prepare("DELETE FROM fcm_tokens WHERE userId = ? AND token = ?");
        $stmt->execute([$data['userId'], $data['token']]);
        echo json_encode(['success' => true, 'message' => 'Token deleted']);
        break;
}
?>
