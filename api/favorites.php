<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');

if ($_SERVER['REQUEST_METHOD'] === 'OPTIONS') {
    http_response_code(200);
    exit();
}

require_once 'db_connection.php';

$method = $_SERVER['REQUEST_METHOD'];

switch ($method) {
    case 'GET':
        if (isset($_GET['id'])) {
            // Get favorite by ID
            $id = $_GET['id'];
            $stmt = $pdo->prepare("SELECT * FROM favorites WHERE id = ?");
            $stmt->execute([$id]);
            $favorite = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($favorite) {
                $favorite['synced'] = (bool)$favorite['synced'];
                echo json_encode($favorite);
            } else {
                echo json_encode(null);
            }
        } elseif (isset($_GET['userId'])) {
            // Get favorites by user ID
            $userId = $_GET['userId'];
            $stmt = $pdo->prepare("SELECT * FROM favorites WHERE userId = ? ORDER BY createdAt DESC");
            $stmt->execute([$userId]);
            $favorites = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($favorites as &$favorite) {
                $favorite['synced'] = (bool)$favorite['synced'];
            }
            echo json_encode($favorites);
        } else {
            // Get all favorites
            $stmt = $pdo->query("SELECT * FROM favorites ORDER BY createdAt DESC");
            $favorites = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($favorites as &$favorite) {
                $favorite['synced'] = (bool)$favorite['synced'];
            }
            echo json_encode($favorites);
        }
        break;
        
    case 'POST':
        // Create new favorite
        $data = json_decode(file_get_contents('php://input'), true);
        
        // Validate required fields
        if (empty($data['userId']) || empty($data['groundId'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields: userId, groundId']);
            break;
        }
        
        $favoriteId = $data['id'] ?? ($data['userId'] . '_' . $data['groundId']);
        $stmt = $pdo->prepare("INSERT INTO favorites (id, userId, groundId, createdAt, synced) VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE createdAt = ?");
        $stmt->execute([
            $favoriteId,
            $data['userId'],
            $data['groundId'],
            $data['createdAt'] ?? time() * 1000,
            1, // synced
            $data['createdAt'] ?? time() * 1000
        ]);
        
        // Return the created favorite
        $stmt = $pdo->prepare("SELECT * FROM favorites WHERE id = ?");
        $stmt->execute([$favoriteId]);
        $favorite = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($favorite) {
            $favorite['synced'] = (bool)$favorite['synced'];
            echo json_encode($favorite);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to retrieve created favorite']);
        }
        break;
        
    case 'DELETE':
        // Delete favorite
        if (!isset($_GET['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Favorite ID is required']);
            break;
        }
        
        $id = $_GET['id'];
        $stmt = $pdo->prepare("DELETE FROM favorites WHERE id = ?");
        $stmt->execute([$id]);
        echo json_encode(['success' => true, 'message' => 'Favorite deleted successfully']);
        break;
}
?>
