# Skeet Skeet - Sports Ground Booking App

A comprehensive Android application for booking sports grounds with offline-first architecture, real-time notifications, and multi-role user management.

## 📱 Overview

**Skeet Skeet** is a full-featured sports ground booking platform that allows users to discover, book, and manage sports ground reservations. The app supports multiple user roles (Players, Groundkeepers, and Admins) and works seamlessly both online and offline.

## ✨ Key Features

### 🔐 Authentication & User Management
- **Firebase Authentication**: Secure email/password authentication
- **Password Reset**: Forgot password functionality
- **User Roles**: Support for Players, Groundkeepers, and Admins
- **Auto-login**: Stay logged in functionality
- **User Profiles**: Manage personal information and preferences

### 🏟️ Ground Management
- **Ground Discovery**: Browse available sports grounds
- **Detailed Information**: View ground details including location, price, amenities, and ratings
- **Image Gallery**: View ground images
- **Search & Filter**: 
  - Search by name, location, or description
  - Filter by location, price range, rating, and amenities (floodlights, parking)
  - Sort by name, price, rating, or location
- **Favorites**: Save favorite grounds for quick access

### 📅 Booking System
- **Easy Booking**: Select date, time, and duration
- **Weather Integration**: View weather forecast for booking dates
- **Conflict Detection**: Prevents double bookings
- **Payment Options**: Support for on-site payment, EasyPaisa, and card payments
- **Booking History**: View past and upcoming bookings
- **Booking Status**: Track booking status (Pending, Confirmed, Cancelled, Completed)

### 📱 Push Notifications
- **Booking Confirmations**: Notifications for booking status changes
- **Reminders**: Booking reminders before scheduled time
- **Weather Alerts**: Weather-based notifications
- **Custom Notifications**: Support for different notification types
- **Firebase Cloud Messaging**: Unlimited push notifications (free tier)

### 🌐 Offline-First Architecture
- **Room Database**: Local SQLite storage for offline access
- **Automatic Sync**: Syncs data to PHP API and Firestore when online
- **Offline Booking**: Create bookings without internet connection
- **Data Persistence**: All data cached locally for instant access
- **Smart Sync**: Automatic conflict resolution and data synchronization

### 🔄 Data Synchronization
- **Multi-Source Sync**: Syncs between Room database, PHP API, and Firestore
- **Background Sync**: Automatic synchronization when connection is restored
- **Conflict Resolution**: Handles data conflicts intelligently
- **Real-time Updates**: Live updates when online

### 👥 Multi-Role Dashboard

#### Player Dashboard
- Browse and search grounds
- View booking history
- Manage favorites
- View weather information
- Profile management

#### Groundkeeper Dashboard
- Manage owned grounds
- View and manage bookings
- Track revenue and statistics
- Settings and preferences

#### Admin Dashboard
- User management
- Ground management
- Analytics and statistics
- System overview

### 🌤️ Weather Integration
- **OpenWeatherMap API**: Current weather and 5-day forecasts
- **Location-based**: Weather information by city or coordinates
- **Booking Context**: Weather forecast for selected booking dates

### 💳 Payment Integration
- **Payment Structure**: Ready for Stripe and PayPal integration
- **Payment Verification**: Track payment status
- **Multiple Methods**: Support for various payment options
- **Note**: Backend implementation required for secure processing

### 🖼️ Image Handling
- **Image Upload**: Upload images to PHP server
- **Image Download**: Retrieve images from server
- **Image Organization**: Organized by type (grounds, users, bookings)
- **Glide Integration**: Efficient image loading and caching

### 📊 Reviews & Ratings
- **User Reviews**: View and submit ground reviews
- **Rating System**: Rate grounds based on experience
- **Review Display**: Show reviews on ground detail pages

## 🏗️ Architecture

### Tech Stack
- **Language**: Kotlin
- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 36
- **Architecture**: MVVM with Repository Pattern

### Key Libraries
- **Firebase**: Authentication, Cloud Messaging, Firestore, Analytics
- **Retrofit**: REST API client for PHP backend
- **Room**: Local SQLite database
- **Glide**: Image loading and caching
- **OkHttp**: HTTP client with logging
- **WorkManager**: Background tasks
- **Navigation Component**: Fragment navigation
- **Material Design**: Modern UI components

### Project Structure
```
app/src/main/java/com/example/smd_fyp/
├── api/              # API clients and services
├── auth/             # Authentication logic
├── database/         # Room database and DAOs
├── firebase/         # Firebase helpers and services
├── fragments/        # UI fragments
├── groundkeeper/     # Groundkeeper-specific features
├── model/            # Data models
├── player/           # Player-specific features
├── sync/             # Synchronization logic
└── utils/            # Utility classes
```

## 🚀 Getting Started

### Prerequisites
- Android Studio (latest version)
- JDK 11 or higher
- Firebase project (free tier)
- XAMPP or PHP server (for backend API)
- OpenWeatherMap API key (optional, for weather features)

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/MominMunir/Skeet-Skeet_FTBS.git
   cd Skeet-Skeet_FTBS
   ```

2. **Firebase Setup**
   - Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
   - Enable Authentication (Email/Password)
   - Enable Cloud Messaging
   - Enable Firestore (start in test mode)
   - Download `google-services.json` and place it in `app/google-services.json`

3. **PHP Backend Setup**
   - Set up XAMPP or your PHP server
   - Configure API base URL in `app/src/main/res/values/strings.xml`
   - Create PHP API endpoints (see `api/` folder for reference)

4. **Weather API (Optional)**
   - Get free API key from [OpenWeatherMap](https://openweathermap.org/api)
   - Add API key to `WeatherApiService.kt`

5. **Build and Run**
   ```bash
   ./gradlew build
   ./gradlew installDebug
   ```

## ⚙️ Configuration

### API Configuration
Update `app/src/main/res/values/strings.xml`:
```xml
<string name="api_ip">192.168.1.100</string> <!-- Your server IP -->
<string name="api_base_url">http://%s/skeetskeet/api/</string>
```

### Firebase Configuration
- Place `google-services.json` in `app/` directory
- No billing account required for basic features

### Weather API
Update `WeatherApiService.kt`:
```kotlin
const val API_KEY = "YOUR_OPENWEATHERMAP_API_KEY"
```

## 📱 Features in Detail

### Offline Functionality
The app works completely offline:
- ✅ View cached grounds
- ✅ Create bookings (synced when online)
- ✅ View booking history
- ✅ Search and filter cached data
- ✅ View user profile

### Online Synchronization
When internet is available:
- ✅ Sync bookings to server
- ✅ Fetch latest ground data
- ✅ Upload images
- ✅ Receive push notifications
- ✅ Sync with Firestore

### User Roles

#### Player
- Browse and search grounds
- Book grounds
- Manage bookings
- Add favorites
- View reviews

#### Groundkeeper
- Manage owned grounds
- View bookings for their grounds
- Accept/reject bookings
- Track revenue
- Update ground information

#### Admin
- Manage all users
- Manage all grounds
- View system analytics
- Monitor bookings
- System administration

## 🔧 Development

### Building the Project
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease
```

### Running Tests
```bash
./gradlew test
```

### Code Structure
- **Models**: Data classes in `model/` package
- **API**: Retrofit services in `api/` package
- **Database**: Room entities and DAOs in `database/` package
- **UI**: Activities and Fragments in respective packages
- **Utils**: Helper classes in `utils/` package

## 📝 API Endpoints

The app expects the following PHP API endpoints:

- `GET /grounds.php` - Get all grounds
- `POST /grounds.php` - Create ground
- `GET /bookings.php` - Get bookings
- `POST /bookings.php` - Create booking
- `POST /upload.php` - Upload image
- `GET /images.php` - Get image URLs
- `GET /users.php` - Get users
- `POST /users.php` - Create/update user

See `api/` folder for PHP template files.

## 🐛 Troubleshooting

### Common Issues

1. **API Connection Failed**
   - Check API base URL in `strings.xml`
   - Ensure PHP server is running
   - Verify network connectivity

2. **Firebase Authentication Not Working**
   - Verify `google-services.json` is in correct location
   - Check Firebase project configuration
   - Ensure Authentication is enabled in Firebase Console

3. **Images Not Loading**
   - Check image URLs in API response
   - Verify PHP upload endpoint is working
   - Check network permissions

4. **Offline Sync Not Working**
   - Verify Room database is initialized
   - Check sync manager is running
   - Review sync logs

## 📄 License

This project is part of a Final Year Project (FYP) for Software Mobile Development (SMD).

## 👥 Contributors

- Abdullah Azeem (22I-1186)
- Talha Khurram (22I-0709)
- Momin Munir (22I-0854)

## 📚 Additional Documentation

For detailed implementation guides, see the `markdown/` directory:
- `FEATURES_IMPLEMENTATION_SUMMARY.md` - Complete feature list
- `IMPLEMENTATION_GUIDE.md` - Setup and usage guide
- `CODE_REVIEW_OFFLINE_SYNC.md` - Offline sync architecture
- `FIREBASE_SETUP_GUIDE.md` - Firebase configuration
- `SQLITE_OFFLINE_STORAGE_GUIDE.md` - Local database guide

## 🎯 Future Enhancements

- [ ] Complete payment gateway integration
- [ ] Advanced analytics dashboard
- [ ] Social features (share bookings, invite friends)
- [ ] In-app chat support
- [ ] Advanced filtering options
- [ ] Booking calendar view
- [ ] Multi-language support

---

**Note**: This app uses Firebase free tier features that don't require a billing account. All core functionality works without any payment setup.

