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
            // Get review by ID
            $id = $_GET['id'];
            $stmt = $pdo->prepare("SELECT * FROM reviews WHERE id = ?");
            $stmt->execute([$id]);
            $review = $stmt->fetch(PDO::FETCH_ASSOC);
            if ($review) {
                $review['synced'] = (bool)$review['synced'];
                echo json_encode($review);
            } else {
                echo json_encode(null);
            }
        } elseif (isset($_GET['groundId'])) {
            // Get reviews by ground ID
            $groundId = $_GET['groundId'];
            $stmt = $pdo->prepare("SELECT * FROM reviews WHERE groundId = ? ORDER BY createdAt DESC");
            $stmt->execute([$groundId]);
            $reviews = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($reviews as &$review) {
                $review['synced'] = (bool)$review['synced'];
            }
            echo json_encode($reviews);
        } elseif (isset($_GET['userId'])) {
            // Get reviews by user ID
            $userId = $_GET['userId'];
            $stmt = $pdo->prepare("SELECT * FROM reviews WHERE userId = ? ORDER BY createdAt DESC");
            $stmt->execute([$userId]);
            $reviews = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($reviews as &$review) {
                $review['synced'] = (bool)$review['synced'];
            }
            echo json_encode($reviews);
        } else {
            // Get all reviews
            $stmt = $pdo->query("SELECT * FROM reviews ORDER BY createdAt DESC");
            $reviews = $stmt->fetchAll(PDO::FETCH_ASSOC);
            foreach ($reviews as &$review) {
                $review['synced'] = (bool)$review['synced'];
            }
            echo json_encode($reviews);
        }
        break;
        
    case 'POST':
        // Create new review
        $data = json_decode(file_get_contents('php://input'), true);
        
        // Validate required fields
        if (empty($data['userId']) || empty($data['groundId']) || !isset($data['rating'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Missing required fields: userId, groundId, rating']);
            break;
        }
        
        $reviewId = $data['id'] ?? uniqid('review_');
        $stmt = $pdo->prepare("INSERT INTO reviews (id, userId, groundId, bookingId, rating, reviewText, createdAt, synced) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        $stmt->execute([
            $reviewId,
            $data['userId'],
            $data['groundId'],
            $data['bookingId'] ?? null,
            $data['rating'],
            $data['reviewText'] ?? null,
            $data['createdAt'] ?? time() * 1000,
            1 // synced
        ]);
        
        // Update ground rating
        $stmt = $pdo->prepare("SELECT AVG(rating) as avgRating FROM reviews WHERE groundId = ?");
        $stmt->execute([$data['groundId']]);
        $result = $stmt->fetch(PDO::FETCH_ASSOC);
        $avgRating = $result['avgRating'] ?? 0.0;
        
        // Update ground with new rating
        $stmt = $pdo->prepare("UPDATE grounds SET rating = ?, ratingText = ? WHERE id = ?");
        $stmt->execute([$avgRating, number_format($avgRating, 1), $data['groundId']]);
        
        // Return the created review
        $stmt = $pdo->prepare("SELECT * FROM reviews WHERE id = ?");
        $stmt->execute([$reviewId]);
        $review = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($review) {
            $review['synced'] = (bool)$review['synced'];
            echo json_encode($review);
        } else {
            http_response_code(500);
            echo json_encode(['success' => false, 'message' => 'Failed to retrieve created review']);
        }
        break;
        
    case 'PUT':
        // Update existing review
        $data = json_decode(file_get_contents('php://input'), true);
        
        if (empty($data['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Review ID is required']);
            break;
        }
        
        $stmt = $pdo->prepare("UPDATE reviews SET rating=?, reviewText=? WHERE id=?");
        $stmt->execute([
            $data['rating'],
            $data['reviewText'] ?? null,
            $data['id']
        ]);
        
        // Update ground rating if groundId is provided
        if (!empty($data['groundId'])) {
            $stmt = $pdo->prepare("SELECT AVG(rating) as avgRating FROM reviews WHERE groundId = ?");
            $stmt->execute([$data['groundId']]);
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
            $avgRating = $result['avgRating'] ?? 0.0;
            
            $stmt = $pdo->prepare("UPDATE grounds SET rating = ?, ratingText = ? WHERE id = ?");
            $stmt->execute([$avgRating, number_format($avgRating, 1), $data['groundId']]);
        }
        
        // Return the updated review
        $stmt = $pdo->prepare("SELECT * FROM reviews WHERE id = ?");
        $stmt->execute([$data['id']]);
        $review = $stmt->fetch(PDO::FETCH_ASSOC);
        if ($review) {
            $review['synced'] = (bool)$review['synced'];
            echo json_encode($review);
        } else {
            http_response_code(404);
            echo json_encode(['success' => false, 'message' => 'Review not found']);
        }
        break;
        
    case 'DELETE':
        // Delete review
        if (!isset($_GET['id'])) {
            http_response_code(400);
            echo json_encode(['success' => false, 'message' => 'Review ID is required']);
            break;
        }
        
        $id = $_GET['id'];
        
        // Get groundId before deleting to update rating
        $stmt = $pdo->prepare("SELECT groundId FROM reviews WHERE id = ?");
        $stmt->execute([$id]);
        $review = $stmt->fetch(PDO::FETCH_ASSOC);
        $groundId = $review['groundId'] ?? null;
        
        // Delete the review
        $stmt = $pdo->prepare("DELETE FROM reviews WHERE id = ?");
        $stmt->execute([$id]);
        
        // Update ground rating if groundId exists
        if ($groundId) {
            $stmt = $pdo->prepare("SELECT AVG(rating) as avgRating FROM reviews WHERE groundId = ?");
            $stmt->execute([$groundId]);
            $result = $stmt->fetch(PDO::FETCH_ASSOC);
            $avgRating = $result['avgRating'] ?? 0.0;
            
            $stmt = $pdo->prepare("UPDATE grounds SET rating = ?, ratingText = ? WHERE id = ?");
            $stmt->execute([$avgRating, number_format($avgRating, 1), $groundId]);
        }
        
        echo json_encode(['success' => true, 'message' => 'Review deleted successfully']);
        break;
}
?>
