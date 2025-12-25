<?php
header('Content-Type: application/json');
header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS');
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
            // Get single ground by ID
            $id = $_GET['id'];
            $stmt = $pdo->prepare("SELECT * FROM grounds WHERE id = ?");
            $stmt->execute([$id]);
            $ground = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($ground) {
                // Convert boolean fields
                $ground['hasFloodlights'] = (bool)$ground['hasFloodlights'];
                $ground['hasParking'] = (bool)$ground['hasParking'];
                $ground['available'] = (bool)$ground['available'];
                $ground['synced'] = (bool)$ground['synced'];
                echo json_encode($ground);
            } else {
                echo json_encode(null);
            }
        } else {
            // Get all grounds
            $stmt = $pdo->query("SELECT * FROM grounds ORDER BY name");
            $grounds = $stmt->fetchAll(PDO::FETCH_ASSOC);
            // Convert boolean fields
            foreach ($grounds as &$ground) {
                $ground['hasFloodlights'] = (bool)$ground['hasFloodlights'];
                $ground['hasParking'] = (bool)$ground['hasParking'];
                $ground['available'] = (bool)$ground['available'];
                $ground['synced'] = (bool)$ground['synced'];
            }
            echo json_encode($grounds);
        }
        break;
        
    case 'POST':
        // Create new ground
        $data = json_decode(file_get_contents('php://input'), true);
        
        // Validate required fields
        if (empty($data['name']) || empty($data['location']) || !isset($data['price'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields: name, location, price']);
            break;
        }
        
        $groundId = $data['id'] ?? uniqid('ground_');
        $stmt = $pdo->prepare("INSERT INTO grounds (id, name, location, price, priceText, rating, ratingText, imageUrl, imagePath, hasFloodlights, hasParking, description, available) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([
            $groundId,
            $data['name'],
            $data['location'],
            $data['price'],
            $data['priceText'] ?? null,
            $data['rating'] ?? 0.0,
            $data['ratingText'] ?? null,
            $data['imageUrl'] ?? null,
            $data['imagePath'] ?? null,
            isset($data['hasFloodlights']) ? (int)$data['hasFloodlights'] : 1,
            isset($data['hasParking']) ? (int)$data['hasParking'] : 1,
            $data['description'] ?? null,
            isset($data['available']) ? (int)$data['available'] : 1
        ]);
        
        // Return the created ground
        $stmt = $pdo->prepare("SELECT * FROM grounds WHERE id = ?");
        $stmt->execute([$groundId]);
        $ground = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($ground) {
            $ground['hasFloodlights'] = (bool)$ground['hasFloodlights'];
            $ground['hasParking'] = (bool)$ground['hasParking'];
            $ground['available'] = (bool)$ground['available'];
            $ground['synced'] = (bool)$ground['synced'];
            echo json_encode($ground);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to retrieve created ground']);
        }
        break;
        
    case 'PUT':
        // Update existing ground
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (empty($data['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Ground ID is required']);
            break;
        }
        
        $stmt = $pdo->prepare("UPDATE grounds SET name=?, location=?, price=?, priceText=?, rating=?, ratingText=?, imageUrl=?, imagePath=?, hasFloodlights=?, hasParking=?, description=?, available=? WHERE id=?");
        $stmt->execute([
            $data['name'],
            $data['location'],
            $data['price'],
            $data['priceText'] ?? null,
            $data['rating'] ?? 0.0,
            $data['ratingText'] ?? null,
            $data['imageUrl'] ?? null,
            $data['imagePath'] ?? null,
            isset($data['hasFloodlights']) ? (int)$data['hasFloodlights'] : 1,
            isset($data['hasParking']) ? (int)$data['hasParking'] : 1,
            $data['description'] ?? null,
            isset($data['available']) ? (int)$data['available'] : 1,
            $data['id']
        ]);
        
        // Return the updated ground
        $stmt = $pdo->prepare("SELECT * FROM grounds WHERE id = ?");
        $stmt->execute([$data['id']]);
        $ground = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($ground) {
            $ground['hasFloodlights'] = (bool)$ground['hasFloodlights'];
            $ground['hasParking'] = (bool)$ground['hasParking'];
            $ground['available'] = (bool)$ground['available'];
            $ground['synced'] = (bool)$ground['synced'];
            echo json_encode($ground);
        } else {
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'Ground not found']);
        }
        break;
        
    case 'DELETE':
        // Delete ground
        if (!isset($_GET['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Ground ID is required']);
            break;
        }
        
        $id = $_GET['id'];
        $stmt = $pdo->prepare("DELETE FROM grounds WHERE id = ?");
        $stmt->execute([$id]);
        echo json_encode(['success' => true, 'message' => 'Ground deleted successfully']);
        break;
}
?>
