<?php
/**
 * Helper function to send FCM push notifications using FCM v1 API (Service Account)
 * 
 * This is the NEW recommended method by Firebase
 * 
 * Usage:
 * require_once 'send_notification_v1.php';
 * sendFCMNotificationV1('user_id', 'Title', 'Message', 'BOOKING', 'related_id');
 */

require_once 'con.php';

// Path to your Service Account JSON file downloaded from Firebase Console
// Go to: Firebase Console → Project Settings → Service Accounts → Generate new private key
// 
// Your API is located at: C:\xampp\htdocs\skeetskeet\api
// JSON file is in the parent directory: C:\xampp\htdocs\skeetskeet\
// Update this path if your JSON file is in a different location
$SERVICE_ACCOUNT_PATH = getenv('FIREBASE_SERVICE_ACCOUNT_PATH') ?: __DIR__ . '/../skeetskeet-5c074-firebase-adminsdk-fbsvc-8585874c62.json';

/**
 * Send notification using FCM v1 API (Service Account method)
 */
function sendFCMNotificationV1($userId, $title, $message, $type = 'SYSTEM', $relatedId = null) {
    global $pdo, $SERVICE_ACCOUNT_PATH;
    
    if (!file_exists($SERVICE_ACCOUNT_PATH)) {
        error_log("Service Account JSON file not found at: " . $SERVICE_ACCOUNT_PATH);
        error_log("Please update \$SERVICE_ACCOUNT_PATH in send_notification_v1.php to point to your JSON file");
        return false;
    }
    
    $serviceAccount = json_decode(file_get_contents($SERVICE_ACCOUNT_PATH), true);
    
    if (!$serviceAccount || !isset($serviceAccount['project_id'])) {
        error_log("Invalid Service Account JSON file. Please check the file format.");
        return false;
    }
    $projectId = $serviceAccount['project_id'];
    
    // Get FCM tokens for the user
    $stmt = $pdo->prepare("SELECT token FROM fcm_tokens WHERE userId = ?");
    $stmt->execute([$userId]);
    $tokens = $stmt->fetchAll(PDO::FETCH_COLUMN);
    
    if (empty($tokens)) {
        error_log("No FCM tokens found for user: $userId");
        return false;
    }
    
    // Get OAuth2 access token
    $accessToken = getAccessToken($serviceAccount);
    if (!$accessToken) {
        error_log("Failed to get access token");
        return false;
    }
    
    $url = "https://fcm.googleapis.com/v1/projects/{$projectId}/messages:send";
    $successCount = 0;
    
    foreach ($tokens as $token) {
        $messageData = [
            'message' => [
                'token' => $token,
                'notification' => [
                    'title' => $title,
                    'body' => $message
                ],
                'data' => [
                    'title' => $title,
                    'message' => $message,
                    'type' => $type,
                    'userId' => $userId,
                    'relatedId' => $relatedId ?: ''
                ],
                'android' => [
                    'priority' => 'high'
                ]
            ]
        ];
        
        $headers = [
            'Authorization: Bearer ' . $accessToken,
            'Content-Type: application/json'
        ];
        
        $ch = curl_init();
        curl_setopt($ch, CURLOPT_URL, $url);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $headers);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
        curl_setopt($ch, CURLOPT_POSTFIELDS, json_encode($messageData));
        
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
                0,
                time() * 1000,
                1
            ]);
        } else {
            error_log("Failed to send FCM notification. HTTP Code: $httpCode, Response: $result");
        }
    }
    
    return $successCount > 0;
}

/**
 * Get OAuth2 access token from Service Account
 */
function getAccessToken($serviceAccount) {
    $now = time();
    $jwt = createJWT($serviceAccount, $now);
    
    $url = 'https://oauth2.googleapis.com/token';
    $data = [
        'grant_type' => 'urn:ietf:params:oauth:grant-type:jwt-bearer',
        'assertion' => $jwt
    ];
    
    $ch = curl_init();
    curl_setopt($ch, CURLOPT_URL, $url);
    curl_setopt($ch, CURLOPT_POST, true);
    curl_setopt($ch, CURLOPT_POSTFIELDS, http_build_query($data));
    curl_setopt($ch, CURLOPT_RETURNTRANSFER, true);
    curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, false);
    
    $result = curl_exec($ch);
    $httpCode = curl_getinfo($ch, CURLINFO_HTTP_CODE);
    curl_close($ch);
    
    if ($httpCode === 200) {
        $response = json_decode($result, true);
        return $response['access_token'] ?? null;
    }
    
    return null;
}

/**
 * Create JWT for OAuth2
 */
function createJWT($serviceAccount, $now) {
    $header = [
        'alg' => 'RS256',
        'typ' => 'JWT'
    ];
    
    $payload = [
        'iss' => $serviceAccount['client_email'],
        'scope' => 'https://www.googleapis.com/auth/firebase.messaging',
        'aud' => 'https://oauth2.googleapis.com/token',
        'exp' => $now + 3600,
        'iat' => $now
    ];
    
    $base64Header = base64UrlEncode(json_encode($header));
    $base64Payload = base64UrlEncode(json_encode($payload));
    
    $signature = '';
    $privateKey = $serviceAccount['private_key'];
    openssl_sign($base64Header . '.' . $base64Payload, $signature, $privateKey, OPENSSL_ALGO_SHA256);
    $base64Signature = base64UrlEncode($signature);
    
    return $base64Header . '.' . $base64Payload . '.' . $base64Signature;
}

function base64UrlEncode($data) {
    return rtrim(strtr(base64_encode($data), '+/', '-_'), '=');
}
?>
