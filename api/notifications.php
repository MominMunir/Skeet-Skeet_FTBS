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
            // Get notification by ID
            $id = $_GET['id'];
            $stmt = $pdo->prepare("SELECT * FROM notifications WHERE id = ?");
            $stmt->execute([$id]);
            $notification = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($notification) {
                $notification['synced'] = (bool)$notification['synced'];
                $notification['isRead'] = (bool)$notification['isRead'];
                echo json_encode($notification);
            } else {
                echo json_encode(null);
            }
        } elseif (isset($_GET['userId'])) {
            // Get notifications by user ID
            $userId = $_GET['userId'];
            $stmt = $pdo->prepare("SELECT * FROM notifications WHERE userId = ? ORDER BY createdAt DESC");
            $stmt->execute([$userId]);
            $notifications = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($notifications as &$notification) {
                $notification['synced'] = (bool)$notification['synced'];
                $notification['isRead'] = (bool)$notification['isRead'];
            }
            echo json_encode($notifications);
        } else {
            // Get all notifications
            $stmt = $pdo->query("SELECT * FROM notifications ORDER BY createdAt DESC");
            $notifications = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($notifications as &$notification) {
                $notification['synced'] = (bool)$notification['synced'];
                $notification['isRead'] = (bool)$notification['isRead'];
            }
            echo json_encode($notifications);
        }
        break;
        
    case 'POST':
        // Create new notification
        $data = json_decode(file_get_contents('php://input'), true);
        
        // Validate required fields
        if (empty($data['userId']) || empty($data['title']) || empty($data['message'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields: userId, title, message']);
            break;
        }
        
        $notificationId = $data['id'] ?? uniqid('notification_');
        $stmt = $pdo->prepare("INSERT INTO notifications (id, userId, title, message, type, relatedId, isRead, createdAt, synced) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([
            $notificationId,
            $data['userId'],
            $data['title'],
            $data['message'],
            $data['type'] ?? 'SYSTEM',
            $data['relatedId'] ?? null,
            isset($data['isRead']) ? (int)$data['isRead'] : 0,
            $data['createdAt'] ?? time() * 1000,
            1 // synced
        ]);
        
        // Return the created notification
        $stmt = $pdo->prepare("SELECT * FROM notifications WHERE id = ?");
        $stmt->execute([$notificationId]);
        $notification = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($notification) {
            $notification['synced'] = (bool)$notification['synced'];
            $notification['isRead'] = (bool)$notification['isRead'];
            echo json_encode($notification);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to retrieve created notification']);
        }
        break;
        
    case 'PUT':
        // Update existing notification
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (empty($data['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Notification ID is required']);
            break;
        }
        
        $stmt = $pdo->prepare("UPDATE notifications SET title=?, message=?, isRead=? WHERE id=?");
        $stmt->execute([
            $data['title'],
            $data['message'],
            isset($data['isRead']) ? (int)$data['isRead'] : 0,
            $data['id']
        ]);
        
        // Return the updated notification
        $stmt = $pdo->prepare("SELECT * FROM notifications WHERE id = ?");
        $stmt->execute([$data['id']]);
        $notification = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($notification) {
            $notification['synced'] = (bool)$notification['synced'];
            $notification['isRead'] = (bool)$notification['isRead'];
            echo json_encode($notification);
        } else {
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'Notification not found']);
        }
        break;
        
    case 'DELETE':
        // Delete notification
        if (!isset($_GET['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Notification ID is required']);
            break;
        }
        
        $id = $_GET['id'];
        $stmt = $pdo->prepare("DELETE FROM notifications WHERE id = ?");
        $stmt->execute([$id]);
        echo json_encode(['success' => true, 'message' => 'Notification deleted successfully']);
        break;
}
?>
