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
            // Get booking by ID
            $id = $_GET['id'];
            $stmt = $pdo->prepare("SELECT * FROM bookings WHERE id = ?");
            $stmt->execute([$id]);
            $booking = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($booking) {
                $booking['synced'] = (bool)$booking['synced'];
                echo json_encode($booking);
            } else {
                echo json_encode(null);
            }
        } elseif (isset($_GET['userId'])) {
            // Get bookings by user ID
            $userId = $_GET['userId'];
            $stmt = $pdo->prepare("SELECT * FROM bookings WHERE userId = ? ORDER BY createdAt DESC");
            $stmt->execute([$userId]);
            $bookings = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($bookings as &$booking) {
                $booking['synced'] = (bool)$booking['synced'];
            }
            echo json_encode($bookings);
        } elseif (isset($_GET['groundId'])) {
            // Get bookings by ground ID
            $groundId = $_GET['groundId'];
            $stmt = $pdo->prepare("SELECT * FROM bookings WHERE groundId = ? ORDER BY createdAt DESC");
            $stmt->execute([$groundId]);
            $bookings = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($bookings as &$booking) {
                $booking['synced'] = (bool)$booking['synced'];
            }
            echo json_encode($bookings);
        } else {
            // Get all bookings
            $stmt = $pdo->query("SELECT * FROM bookings ORDER BY createdAt DESC");
            $bookings = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($bookings as &$booking) {
                $booking['synced'] = (bool)$booking['synced'];
            }
            echo json_encode($bookings);
        }
        break;
        
    case 'POST':
        // Create new booking
        $data = json_decode(file_get_contents('php://input'), true);
        
        // Validate required fields
        if (empty($data['userId']) || empty($data['groundId']) || empty($data['groundName']) || 
            empty($data['date']) || empty($data['time']) || !isset($data['totalPrice'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields: userId, groundId, groundName, date, time, totalPrice']);
            break;
        }
        
        $bookingId = $data['id'] ?? uniqid('booking_');
        $stmt = $pdo->prepare("INSERT INTO bookings (id, userId, groundId, groundName, date, time, duration, totalPrice, status, paymentId, paymentStatus, paymentMethod, createdAt, updatedAt) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([
            $bookingId,
            $data['userId'],
            $data['groundId'],
            $data['groundName'],
            $data['date'],
            $data['time'],
            $data['duration'] ?? 1,
            $data['totalPrice'],
            $data['status'] ?? 'PENDING',
            $data['paymentId'] ?? null,
            $data['paymentStatus'] ?? 'PENDING',
            $data['paymentMethod'] ?? null,
            $data['createdAt'] ?? time() * 1000,
            $data['updatedAt'] ?? time() * 1000
        ]);
        
        // Return the created booking
        $stmt = $pdo->prepare("SELECT * FROM bookings WHERE id = ?");
        $stmt->execute([$bookingId]);
        $booking = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($booking) {
            $booking['synced'] = (bool)$booking['synced'];
            
            // Send notification to user about booking creation
            require_once 'send_notification_v1.php';
            $title = "Booking Created";
            $message = "Your booking at {$data['groundName']} has been created for {$data['date']} at {$data['time']}";
            sendFCMNotificationV1($data['userId'], $title, $message, 'BOOKING', $bookingId);
            
            echo json_encode($booking);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to retrieve created booking']);
        }
        break;
        
    case 'PUT':
        // Update existing booking
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (empty($data['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Booking ID is required']);
            break;
        }
        
        // Get current booking to check status change
        $stmt = $pdo->prepare("SELECT * FROM bookings WHERE id = ?");
        $stmt->execute([$data['id']]);
        $oldBooking = $stmt->fetch(PDO::FETCH_ASSOC);
        $oldStatus = $oldBooking['status'] ?? 'PENDING';
        
        $stmt = $pdo->prepare("UPDATE bookings SET groundName=?, date=?, time=?, duration=?, totalPrice=?, status=?, paymentId=?, paymentStatus=?, paymentMethod=?, updatedAt=? WHERE id=?");
        $stmt->execute([
            $data['groundName'],
            $data['date'],
            $data['time'],
            $data['duration'] ?? 1,
            $data['totalPrice'],
            $data['status'],
            $data['paymentId'] ?? null,
            $data['paymentStatus'] ?? 'PENDING',
            $data['paymentMethod'] ?? null,
            time() * 1000,
            $data['id']
        ]);
        
        // Return the updated booking
        $stmt = $pdo->prepare("SELECT * FROM bookings WHERE id = ?");
        $stmt->execute([$data['id']]);
        $booking = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($booking) {
            $booking['synced'] = (bool)$booking['synced'];
            
            // Send notification if status changed
            if ($oldStatus !== $data['status']) {
                require_once 'send_notification_v1.php';
                $userId = $booking['userId'];
                $groundName = $booking['groundName'];
                $newStatus = $data['status'];
                
                if ($newStatus === 'CONFIRMED') {
                    $title = "Booking Confirmed";
                    $message = "Your booking at $groundName has been confirmed for {$booking['date']} at {$booking['time']}";
                    sendFCMNotificationV1($userId, $title, $message, 'BOOKING', $data['id']);
                } elseif ($newStatus === 'CANCELLED') {
                    $title = "Booking Cancelled";
                    $message = "Your booking at $groundName has been cancelled";
                    sendFCMNotificationV1($userId, $title, $message, 'BOOKING', $data['id']);
                }
            }
            
            // Send notification if payment status changed
            if (isset($data['paymentStatus']) && $oldBooking['paymentStatus'] !== $data['paymentStatus']) {
                if ($data['paymentStatus'] === 'PAID') {
                    require_once 'send_notification_v1.php';
                    $title = "Payment Received";
                    $message = "Payment of Rs. {$booking['totalPrice']} has been received for your booking";
                    sendFCMNotificationV1($booking['userId'], $title, $message, 'PAYMENT', $data['id']);
                }
            }
            
            echo json_encode($booking);
        } else {
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'Booking not found']);
        }
        break;
        
    case 'DELETE':
        // Delete booking
        if (!isset($_GET['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Booking ID is required']);
            break;
        }
        
        $id = $_GET['id'];
        $stmt = $pdo->prepare("DELETE FROM bookings WHERE id = ?");
        $stmt->execute([$id]);
        echo json_encode(['success' => true, 'message' => 'Booking deleted successfully']);
        break;
}
?>
