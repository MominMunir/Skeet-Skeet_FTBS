# Complete Code Review: Functionality & Offline Sync Implementation

## 📋 Table of Contents
1. [Overview](#overview)
2. [Where Functionality is Implemented](#where-functionality-is-implemented)
3. [Offline Sync Implementation](#offline-sync-implementation)
4. [What Works When WiFi is Turned Off](#what-works-when-wifi-is-turned-off)
5. [Data Flow Diagrams](#data-flow-diagrams)

---

## Overview

This Android application uses an **offline-first architecture** with:
- **Local Storage**: SQLite database via Room (works completely offline)
- **Sync Layer**: `SyncManager` handles synchronization between local DB, PHP API, and Firestore
- **Network Detection**: `SyncManager.isOnline()` checks connectivity before syncing

---

## Where Functionality is Implemented

### 1. **Authentication & User Management**

#### Location: `app/src/main/java/com/example/smd_fyp/auth/`
- **LoginFragment.kt**: Email/password login via Firebase
- **SignupFragment.kt**: User registration
- **ResetPasswordActivity.kt**: Password reset functionality
- **LoginStateManager.kt**: Manages "stay logged in" state

**Offline Behavior**: 
- Login/Signup requires internet (Firebase Auth)
- Once logged in, user data is cached locally

---

### 2. **Home Screen & Grounds Listing**

#### Location: `app/src/main/java/com/example/smd_fyp/HomeActivity.kt`

**Functionality**:
- Displays list of available grounds
- Search and filter by location, price
- Navigation drawer with profile, bookings, favorites, settings
- Weather button navigation

**Data Loading** (Lines 238-272):
```kotlin
// First, observe local database (offline support)
LocalDatabaseHelper.getAllGrounds()?.collect { localGrounds ->
    allGrounds = localGrounds.filter { it.available }
    applyFilters()
}

// Fetch from API if online
if (SyncManager.isOnline(this@HomeActivity)) {
    // Fetch and save to local DB
}
```

**Offline Behavior**: ✅ Works offline - shows cached grounds from local database

---

### 3. **Ground Details**

#### Location: `app/src/main/java/com/example/smd_fyp/GroundDetailActivity.kt`

**Functionality**:
- View ground details (name, location, price, rating, amenities)
- View reviews
- Book ground button
- Add to favorites

**Offline Behavior**: ✅ Works offline - loads ground data from local database

---

### 4. **Booking System**

#### Location: `app/src/main/java/com/example/smd_fyp/BookingActivity.kt`

**Functionality**:
- Select date, time, duration
- Weather forecast for selected date
- Payment method selection (On-site, EasyPaisa, Card)
- Booking conflict detection
- Create booking

**Key Implementation** (Lines 355-481):
```kotlin
private fun createBooking() {
    // 1. Save to local database first (works offline)
    LocalDatabaseHelper.saveBooking(booking.copy(synced = false))
    
    // 2. Create local notification
    LocalDatabaseHelper.saveNotification(notification)
    
    // 3. Sync to API if online
    if (SyncManager.isOnline(this@BookingActivity)) {
        SyncManager.syncBooking(this@BookingActivity, booking)
    } else {
        Toast.makeText(this, "Booking saved locally. Will sync when online.", ...)
    }
}
```

**Offline Behavior**: ✅ Works offline - booking is saved locally, syncs when online

---

### 5. **My Bookings**

#### Location: `app/src/main/java/com/example/smd_fyp/MyBookingsActivity.kt`

**Functionality**:
- View all user bookings
- View receipt
- Submit reviews

**Data Loading** (Lines 235-278):
```kotlin
// First, observe local database (offline support)
LocalDatabaseHelper.getBookingsByUser(currentUser.uid)?.collect { localBookings ->
    bookingAdapter.updateItems(localBookings)
}

// Fetch from API if online
if (SyncManager.isOnline(this@MyBookingsActivity)) {
    // Fetch and save to local DB
}
```

**Offline Behavior**: ✅ Works offline - shows cached bookings from local database

---

### 6. **Favorites**

#### Location: `app/src/main/java/com/example/smd_fyp/fragments/FavoritesFragment.kt`

**Functionality**:
- View favorite grounds
- Remove favorites

**Data Loading** (Lines 64-132):
```kotlin
// Observe local database (offline support)
LocalDatabaseHelper.getFavoritesByUser(currentUser.uid)?.collect { localFavorites ->
    // Display favorites
}

// Fetch from API if online
if (SyncManager.isOnline(currentContext)) {
    // Sync favorites
}
```

**Offline Behavior**: ✅ Works offline - shows cached favorites from local database

---

### 7. **Reviews**

#### Location: `app/src/main/java/com/example/smd_fyp/ReviewDialog.kt`

**Functionality**:
- Submit reviews with rating and text
- Reviews update ground ratings

**Offline Behavior**: ✅ Works offline - reviews saved locally, syncs when online

---

### 8. **Notifications**

#### Location: `app/src/main/java/com/example/smd_fyp/fragments/NotificationsFragment.kt`

**Functionality**:
- View notifications
- Mark as read
- Unread count badge

**Offline Behavior**: ✅ Works offline - notifications stored locally

---

### 9. **User Profile**

#### Location: `app/src/main/java/com/example/smd_fyp/UserProfileActivity.kt`

**Functionality**:
- View/edit profile
- Change password
- View favorites, bookings, settings

**Offline Behavior**: ✅ Works offline - profile data cached locally

---

### 10. **Admin Dashboard**

#### Location: `app/src/main/java/com/example/smd_fyp/AdminDashboardActivity.kt`

**Functionality**:
- Overview statistics
- Manage users
- Manage grounds
- View bookings
- Analytics
- Send notifications

**Offline Behavior**: ⚠️ Limited offline - statistics require API, but can view cached data

---

### 11. **Groundkeeper Dashboard**

#### Location: `app/src/main/java/com/example/smd_fyp/GroundkeeperDashboardActivity.kt`

**Functionality**:
- Overview statistics
- Manage own grounds
- View bookings for own grounds
- Settings

**Data Loading** (Lines 65-92):
```kotlin
// Fetch from API first if online
if (SyncManager.isOnline(this@GroundkeeperDashboardActivity)) {
    // Fetch bookings and save to local DB
}
```

**Offline Behavior**: ⚠️ Limited offline - statistics require API, but can view cached bookings

---

### 12. **Weather**

#### Location: `app/src/main/java/com/example/smd_fyp/WeatherActivity.kt`

**Functionality**:
- Current weather
- 5-day forecast
- Weather-based booking recommendations

**Offline Behavior**: ❌ Requires internet - uses OpenMeteo API

---

## Offline Sync Implementation

### Core Sync Manager

#### Location: `app/src/main/java/com/example/smd_fyp/sync/SyncManager.kt`

**Purpose**: Centralized sync manager that handles data synchronization between:
- Local SQLite (Room Database)
- PHP API (XAMPP server)
- Firestore (cloud backup)

---

### 1. **Network Detection**

**Location**: `SyncManager.kt` (Lines 30-36)
```kotlin
fun isOnline(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
```

**Usage**: Called before any sync operation to check if device is online.

---

### 2. **Sync All Data**

**Location**: `SyncManager.kt` (Lines 41-102)
```kotlin
suspend fun syncAll(context: Context): Result<SyncResult>
```

**What it syncs**:
- All unsynced bookings (`getUnsyncedBookings()`)
- All unsynced grounds (`getUnsyncedGrounds()`)
- All unsynced users (`getUnsyncedUsers()`)

**Sync Flow**:
1. Check if online
2. Get all unsynced items from local database
3. For each item, sync to PHP API
4. If PHP sync succeeds, sync to Firestore (optional)
5. Mark as synced in local database

**Where it's called**:
- Manual sync button (if implemented)
- On app resume (if implemented)
- When connection restored (if network callback is registered)

---

### 3. **Sync Individual Items**

#### **Sync Booking**
**Location**: `SyncManager.kt` (Lines 108-189)

**Flow**:
1. Save to local SQLite first (works offline)
2. If offline, return success (data saved locally)
3. If online:
   - Check if booking exists in PHP
   - Create or update in PHP API
   - Sync to Firestore (optional)
   - Mark as synced in local DB

**Where it's called**:
- `BookingActivity.kt` (Line 440): After creating booking
- `AutoBookingStatusWorker.kt`: Periodic status updates

#### **Sync Ground**
**Location**: `SyncManager.kt` (Lines 195-275)

**Flow**: Same as booking sync

**Where it's called**:
- `GroundkeeperAddGroundFragment.kt`: After creating ground
- `GroundkeeperEditGroundFragment.kt`: After updating ground
- `HomeActivity.kt` (Line 263): After fetching from API

#### **Sync User**
**Location**: `SyncManager.kt` (Lines 281-370)

**Flow**: Same as booking sync

**Where it's called**:
- `SignupFragment.kt`: After user registration
- `EditProfileFragment.kt`: After profile update

#### **Sync Review**
**Location**: `SyncManager.kt` (Lines 565-630)

**Flow**: 
1. Save to local SQLite
2. If online, sync to PHP API
3. Update ground rating after sync

**Where it's called**:
- `ReviewDialog.kt`: After submitting review

#### **Sync Notification**
**Location**: `SyncManager.kt` (Lines 636-698)

**Flow**: Same as booking sync

**Where it's called**:
- `BookingActivity.kt` (Line 433): After creating booking notification
- `FirebaseMessagingService.kt`: When receiving push notification

#### **Sync Favorite**
**Location**: `SyncManager.kt` (Lines 704-746)

**Flow**: Same as booking sync

**Where it's called**:
- `GroundDetailActivity.kt`: When adding/removing favorite

---

### 4. **Local Database Helper**

#### Location: `app/src/main/java/com/example/smd_fyp/database/LocalDatabaseHelper.kt`

**Purpose**: Wrapper around Room Database for easy access

**Key Methods**:
- `saveBooking()`: Save booking to local DB
- `getBookingsByUser()`: Get bookings (returns Flow for reactive updates)
- `getUnsyncedBookings()`: Get bookings that need syncing
- `markBookingAsSynced()`: Mark booking as synced

**Same pattern for**: Grounds, Users, Reviews, Notifications, Favorites

---

### 5. **Automatic Sync Workers**

#### **Auto Booking Status Worker**
**Location**: `app/src/main/java/com/example/smd_fyp/sync/AutoBookingStatusWorker.kt`

**Purpose**: Periodically updates booking statuses (PENDING → CONFIRMED/CANCELLED)

**Scheduled in**: `HomeActivity.kt` (Lines 301-315)
```kotlin
val work = PeriodicWorkRequestBuilder<AutoBookingStatusWorker>(15, TimeUnit.MINUTES)
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .build()
```

**Runs**: Every 15 minutes (only when online)

---

#### **Weather Notification Worker**
**Location**: `app/src/main/java/com/example/smd_fyp/sync/WeatherNotificationWorker.kt`

**Purpose**: Checks weather and sends notifications for upcoming bookings

**Scheduled in**: `HomeActivity.kt` (Lines 317-332)
```kotlin
val work = PeriodicWorkRequestBuilder<WeatherNotificationWorker>(24, TimeUnit.HOURS)
    .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
    .build()
```

**Runs**: Every 24 hours (only when online)

---

## What Works When WiFi is Turned Off

### ✅ **Fully Functional (Offline)**

1. **View Grounds**
   - Location: `HomeActivity.kt`
   - Shows cached grounds from local database
   - Filters work (location, price)
   - Search works (on cached data)

2. **View Ground Details**
   - Location: `GroundDetailActivity.kt`
   - Shows cached ground information
   - Shows cached reviews
   - Can add to favorites (saved locally)

3. **Create Booking**
   - Location: `BookingActivity.kt`
   - All booking creation works
   - Conflict detection works (checks local database)
   - Booking saved to local database
   - Notification created locally
   - **Note**: Weather forecast requires internet

4. **View My Bookings**
   - Location: `MyBookingsActivity.kt`, `BookingsFragment.kt`
   - Shows all cached bookings
   - Can view receipt (if cached)
   - Can submit reviews (saved locally)

5. **View Favorites**
   - Location: `FavoritesFragment.kt`
   - Shows cached favorites
   - Can remove favorites (saved locally)

6. **View Notifications**
   - Location: `NotificationsFragment.kt`
   - Shows cached notifications
   - Can mark as read (saved locally)

7. **View Profile**
   - Location: `UserProfileActivity.kt`
   - Shows cached profile data
   - Can edit profile (saved locally, syncs when online)

8. **View Reviews**
   - Shows cached reviews
   - Can submit reviews (saved locally)

9. **Navigation**
   - All navigation works
   - Drawer menu works
   - All screens accessible

---

### ⚠️ **Partially Functional (Limited Offline)**

1. **Admin Dashboard**
   - Can view cached data
   - Statistics may be outdated
   - Cannot fetch latest data

2. **Groundkeeper Dashboard**
   - Can view cached bookings
   - Statistics may be outdated
   - Cannot fetch latest data

---

### ❌ **Requires Internet**

1. **Weather Data**
   - Location: `WeatherActivity.kt`, `BookingActivity.kt`
   - Weather forecast requires OpenMeteo API
   - Shows "Unable to load weather" when offline

2. **Login/Signup**
   - Location: `LoginFragment.kt`, `SignupFragment.kt`
   - Requires Firebase Authentication
   - Cannot login/signup offline

3. **Password Reset**
   - Location: `ResetPasswordActivity.kt`
   - Requires Firebase Authentication
   - Cannot reset password offline

4. **Image Loading**
   - New images require internet
   - Cached images work offline (via Glide)

5. **Initial Data Load**
   - If app is opened for first time offline, no data will be available
   - Requires at least one online session to cache data

---

## Data Flow Diagrams

### Creating a Booking (Offline-First)

```
┌─────────────────┐
│   User Action   │
│  (Create Booking)│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│  Save to SQLite │  ← Works offline!
│  (Local Storage)│
│  synced = false │
└────────┬────────┘
         │
         ├─────────────────┐
         │                 │
         ▼                 ▼
┌─────────────────┐  ┌──────────────┐
│  PHP API Sync   │  │ Firestore    │
│  (If Online)    │  │ (If Online)  │
└────────┬────────┘  └──────────────┘
         │
         ▼
┌─────────────────┐
│ Mark as Synced  │
│  in SQLite      │
│  synced = true  │
└─────────────────┘
```

### Loading Grounds (Offline-First)

```
┌─────────────────┐
│  User Opens     │
│  Home Screen    │
└────────┬────────┘
         │
         ├─────────────────┐
         │                 │
         ▼                 ▼
┌─────────────────┐  ┌──────────────┐
│ Load from SQLite│  │ Fetch from   │
│ (Instant, Works │  │ PHP API      │
│  Offline)       │  │ (If Online)  │
└────────┬────────┘  └──────┬───────┘
         │                  │
         │                  ▼
         │         ┌─────────────────┐
         │         │ Save to SQLite   │
         │         │ (Update Cache)   │
         │         └─────────────────┘
         │
         ▼
┌─────────────────┐
│  Display in UI  │
│  (Reactive)     │
└─────────────────┘
```

### Sync When Connection Restored

```
┌─────────────────┐
│ Connection      │
│ Restored        │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Check for       │
│ Unsynced Data   │
│ (synced = false)│
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Sync Each Item  │
│ to PHP API      │
└────────┬────────┘
         │
         ├─────────────────┐
         │                 │
         ▼                 ▼
┌─────────────────┐  ┌──────────────┐
│ Sync to         │  │ Mark as      │
│ Firestore       │  │ Synced       │
│ (Optional)      │  │ in SQLite    │
└─────────────────┘  └──────────────┘
```

---

## Summary

### Offline Sync Points

1. **SyncManager.syncAll()** - Syncs all unsynced data
   - Called manually or on connection restore
   - Location: `SyncManager.kt` (Line 41)

2. **SyncManager.syncBooking()** - Syncs individual booking
   - Called after creating booking
   - Location: `BookingActivity.kt` (Line 440)

3. **SyncManager.syncGround()** - Syncs individual ground
   - Called after creating/updating ground
   - Location: Various groundkeeper fragments

4. **SyncManager.syncUser()** - Syncs user data
   - Called after signup/profile update
   - Location: `SignupFragment.kt`, `EditProfileFragment.kt`

5. **SyncManager.syncReview()** - Syncs review
   - Called after submitting review
   - Location: `ReviewDialog.kt`

6. **SyncManager.syncNotification()** - Syncs notification
   - Called after creating notification
   - Location: `BookingActivity.kt`, `FirebaseMessagingService.kt`

7. **SyncManager.syncFavorite()** - Syncs favorite
   - Called after adding favorite
   - Location: `GroundDetailActivity.kt`

### What Works Offline

✅ **Viewing**: Grounds, Bookings, Favorites, Notifications, Profile, Reviews
✅ **Creating**: Bookings, Reviews, Favorites (saved locally, syncs when online)
✅ **Editing**: Profile (saved locally, syncs when online)
✅ **Filtering**: Grounds by location, price (on cached data)
✅ **Navigation**: All screens accessible

### What Requires Internet

❌ **Authentication**: Login, Signup, Password Reset
❌ **Weather**: Weather forecast and notifications
❌ **Initial Load**: First-time app usage requires internet to fetch data
❌ **Image Loading**: New images (cached images work offline)

---

## Recommendations

1. **Add Network Callback**: Register `ConnectivityManager.NetworkCallback` to auto-sync when connection is restored
2. **Add Manual Sync Button**: In settings, allow users to manually trigger sync
3. **Show Sync Status**: Display indicator when data is unsynced
4. **Queue Failed Syncs**: Retry failed syncs automatically when online
5. **Offline Indicator**: Show banner when offline to inform users

---

**Last Updated**: Based on code review of current codebase
**Review Date**: 2025-01-XX

