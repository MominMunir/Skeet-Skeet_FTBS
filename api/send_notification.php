<?php
/**
 * Helper function to send FCM push notifications using Legacy API
 * 
 * NOTE: This uses the LEGACY FCM API. If you can't find Server Key in Firebase Console,
 * use send_notification_v1.php instead (uses Service Account - recommended method)
 * 
 * Usage:
 * require_once 'send_notification.php';
 * sendFCMNotification('user_id', 'Title', 'Message', 'BOOKING', 'related_id');
 */

require_once 'db_connection.php';

// Get Firebase Server Key from environment or config
// TODO: Replace with your actual Firebase Server Key from Firebase Console
// Go to: Firebase Console → Project Settings → Cloud Messaging → Cloud Messaging API (Legacy)
// NOTE: If you don't see "Server Key", Firebase may have disabled legacy API for your project
// In that case, use send_notification_v1.php with Service Account instead
// 
// - Server Key: Used for sending notifications from PHP (Legacy method)
// - Sender ID: Numeric ID for client-side (already in google-services.json)
// - Service Account: New recommended method (see send_notification_v1.php)
$FCM_SERVER_KEY = getenv('FCM_SERVER_KEY') ?: 'YOUR_SERVER_KEY_HERE';

function sendFCMNotification($userId, $title, $message, $type = 'SYSTEM', $relatedId = null) {
    global $pdo, $FCM_SERVER_KEY;
    
    if ($FCM_SERVER_KEY === 'YOUR_SERVER_KEY_HERE') {
        error_log("FCM Server Key not configured. Please set FCM_SERVER_KEY in send_notification.php");
        return false;
    }
    
    // Get FCM tokens for the user
    $stmt = $pdo->prepare("SELECT token FROM fcm_tokens WHERE userId = ?");
    $stmt->execute([$userId]);
    $tokens = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    if (empty($tokens)) {
        error_log("No FCM tokens found for user: $userId");
        return false; // User has no registered tokens
    }
    
    $url = 'https://fcm.googleapis.com/fcm/send';
    $successCount = 0;
    
    foreach ($tokens as $token) {
        $notification = [
            'title' => $title,
            'body' => $message,
            'sound' => 'default',
            'click_action' => 'FLUTTER_NOTIFICATION_CLICK' // For Android
        ];
        
        $data = [
            'title' => $title,
            'message' => $message,
            'body' => $message, // Alternative field name
            'type' => $type,
            'userId' => $userId,
            'relatedId' => $relatedId ?: ''
        ];
        
        $fields = [
            'to' => $token,
            'notification' => $notification,
            'data' => $data,
            'priority' => 'high'
        ];
        
        $headers = [
            'Authorization: key=' . $FCM_SERVER_KEY,
            'Content-Type: application/json'
        ];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($fields));
        
        $result = curl_exec($ch);
        $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        curl_close($ch);
        
        if ($httpCode === 200) {
            $successCount++;
            
            // Save notification to database
            $notificationId = uniqid('notification_');
            $stmt = $pdo->prepare("INSERT INTO notifications (id, userId, title, message, type, relatedId, isRead, createdAt, synced) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            $stmt->execute([
                $notificationId,
                $userId,
                $title,
                $message,
                $type,
                $relatedId,
                0, // isRead = false
                time() * 1000,
                1 // synced = true
            ]);
        } else {
            error_log("Failed to send FCM notification. HTTP Code: $httpCode, Response: $result");
        }
    }
    
    return $successCount > 0;
}

/**
 * Send notification to multiple users
 */
function sendFCMNotificationToMultiple($userIds, $title, $message, $type = 'SYSTEM', $relatedId = null) {
    $successCount = 0;
    foreach ($userIds as $userId) {
        if (sendFCMNotification($userId, $title, $message, $type, $relatedId)) {
            $successCount++;
        }
    }
    return $successCount;
}

/**
 * Example: Send booking confirmation notification
 */
function sendBookingConfirmationNotification($userId, $groundName, $bookingId, $date, $time) {
    $title = "Booking Confirmed";
    $message = "Your booking at $groundName has been confirmed for $date at $time";
    return sendFCMNotification($userId, $title, $message, 'BOOKING', $bookingId);
}

/**
 * Example: Send payment notification
 */
function sendPaymentNotification($userId, $amount, $bookingId) {
    $title = "Payment Received";
    $message = "Payment of Rs. $amount has been received for your booking";
    return sendFCMNotification($userId, $title, $message, 'PAYMENT', $bookingId);
}

/**
 * Example: Send booking reminder
 */
function sendBookingReminder($userId, $groundName, $bookingId, $timeUntil) {
    $title = "Booking Reminder";
    $message = "You have a booking at $groundName in $timeUntil";
    return sendFCMNotification($userId, $title, $message, 'REMINDER', $bookingId);
}
?>
