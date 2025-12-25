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
    case 'GET':
        if (isset($_GET['id'])) {
            // Get user by ID
            $id = $_GET['id'];
            $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
            $stmt->execute([$id]);
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($user) {
                // Convert boolean fields
                $user['synced'] = (bool)$user['synced'];
                echo json_encode($user);
            } else {
                echo json_encode(null);
            }
        } elseif (isset($_GET['email'])) {
            // Get user by email
            $email = $_GET['email'];
            $stmt = $pdo->prepare("SELECT * FROM users WHERE email = ?");
            $stmt->execute([$email]);
            $user = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($user) {
                $user['synced'] = (bool)$user['synced'];
                echo json_encode($user);
            } else {
                echo json_encode(null);
            }
        } else {
            // Get all users
            $stmt = $pdo->query("SELECT * FROM users ORDER BY createdAt DESC");
            $users = $stmt->fetchAll(PDO::FETCH_ASSOC);
            // Convert boolean fields
            foreach ($users as &$user) {
                $user['synced'] = (bool)$user['synced'];
            }
            echo json_encode($users);
        }
        break;
        
    case 'POST':
        // Create new user
        $data = json_decode(file_get_contents('php://input'), true);
        
        // Validate required fields
        if (empty($data['id']) || empty($data['email']) || empty($data['fullName'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields: id, email, fullName']);
            break;
        }
        
        $userId = $data['id'];
        $stmt = $pdo->prepare("INSERT INTO users (id, email, fullName, role, phoneNumber, profileImageUrl, createdAt) VALUES (?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([
            $userId,
            $data['email'],
            $data['fullName'],
            $data['role'] ?? 'PLAYER',
            $data['phoneNumber'] ?? null,
            $data['profileImageUrl'] ?? null,
            $data['createdAt'] ?? time() * 1000
        ]);
        
        // Return the created user
        $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
        $stmt->execute([$userId]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($user) {
            $user['synced'] = (bool)$user['synced'];
            echo json_encode($user);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to retrieve created user']);
        }
        break;
        
    case 'PUT':
        // Update existing user
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (empty($data['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'User ID is required']);
            break;
        }
        
        $stmt = $pdo->prepare("UPDATE users SET email=?, fullName=?, role=?, phoneNumber=?, profileImageUrl=? WHERE id=?");
        $stmt->execute([
            $data['email'],
            $data['fullName'],
            $data['role'],
            $data['phoneNumber'] ?? null,
            $data['profileImageUrl'] ?? null,
            $data['id']
        ]);
        
        // Return the updated user
        $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
        $stmt->execute([$data['id']]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($user) {
            $user['synced'] = (bool)$user['synced'];
            echo json_encode($user);
        } else {
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'User not found']);
        }
        break;
        
    case 'DELETE':
        // Delete user
        if (!isset($_GET['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'User ID is required']);
            break;
        }
        
        $id = $_GET['id'];
        
        // Check if user exists
        $stmt = $pdo->prepare("SELECT * FROM users WHERE id = ?");
        $stmt->execute([$id]);
        $user = $stmt->fetch(PDO::FETCH_ASSOC);
        
        if (!$user) {
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'User not found']);
            break;
        }
        
        // Delete user (cascade will handle related bookings)
        $stmt = $pdo->prepare("DELETE FROM users WHERE id = ?");
        $stmt->execute([$id]);
        
        echo json_encode(['success' => true, 'message' => 'User deleted successfully']);
        break;
}
?>
