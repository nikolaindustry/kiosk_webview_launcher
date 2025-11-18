# Kiosk WebView Launcher

An Android kiosk mode application that displays web content in a locked-down, full-screen WebView. Perfect for dedicated devices, digital signage, point-of-sale systems, or any scenario requiring a secure web-based kiosk interface.

## Features

- **Kiosk Mode**: Full-screen WebView with hidden navigation bars
- **Home Launcher**: Set as default home screen to prevent access to other apps
- **Password Protection**: Secure settings access with SHA-256 encrypted passwords
- **URL Configuration**: Easy web URL configuration through settings
- **Hidden Settings Access**: 11-tap gesture to access settings (discreet and secure)
- **Manual Refresh**: 3-finger swipe down to reload the webpage
- **Full Web Support**: 
  - JavaScript enabled
  - Payment processing support
  - Popup windows and redirects
  - File uploads
  - Geolocation
  - Camera and microphone access
  - Cookies and session management
- **Auto URL Reload**: Automatically loads updated URL when returning from settings

## Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/nikolaindustry/kiosk_webview_launcher.git
   ```

2. Open the project in Android Studio

3. Build and install on your Android device (API 24+)

4. Set the app as your default home launcher when prompted

## Usage

### Initial Setup

1. Launch the app
2. Grant all requested permissions
3. Tap the screen 11 times rapidly to access settings
4. Enter default password: `12345`
5. Configure your desired web URL
6. Click "Save URL"

### Accessing Settings

- **Method 1**: Tap the screen 11 times within 3 seconds
- **Method 2**: Long-press the Volume Down button

### Refreshing the Page

Swipe down with 3 fingers to manually reload the webpage.

### Changing Password

1. Access settings with current password
2. Scroll down to "Change Password" section
3. Enter new password (minimum 4 characters)
4. Confirm the password
5. Click "Update Password"

## Configuration

### Default Settings

- **Default URL**: `https://www.nikolaindustry.com`
- **Default Password**: `12345`
- **Tap Threshold**: 11 taps
- **Tap Timeout**: 3 seconds
- **Refresh Gesture**: 3-finger swipe down

### Permissions

The app requests the following permissions for full web functionality:

- Internet access
- Network state
- Camera and microphone (for web features)
- Location services
- Storage access (for file uploads/downloads)
- System alert window (for kiosk mode)

## Requirements

- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 14 (API 34)
- **Build Tools**: Gradle 8.10
- **Android Gradle Plugin**: 8.5.0
- **Java**: 21

## Build Configuration

```gradle
android {
    compileSdk 34
    
    defaultConfig {
        minSdk 24
        targetSdk 34
    }
}
```

## Security Features

- SHA-256 password hashing
- No plain-text password storage
- Session-based authentication
- Secure SharedPreferences storage

## Project Structure

```
app/
├── src/main/
│   ├── java/com/kiosk/webviewlauncher/
│   │   ├── MainActivity.java          # Main WebView activity
│   │   └── SettingsActivity.java      # Settings and configuration
│   ├── res/
│   │   ├── layout/                    # UI layouts
│   │   ├── values/                    # Strings, themes
│   │   └── xml/                       # File paths configuration
│   └── AndroidManifest.xml            # App configuration
```

## Troubleshooting

### App doesn't stay in foreground
- Ensure the app is set as the default home launcher
- Check that all kiosk mode permissions are granted

### Settings won't open
- Ensure you're tapping quickly (all 11 taps within 3 seconds)
- Try long-pressing Volume Down as alternative

### Webpage features not working
- Verify all permissions are granted
- Check internet connectivity
- Ensure JavaScript is enabled (it is by default)

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues, questions, or suggestions, please open an issue on GitHub.

## Author

Nikola Industry

## Acknowledgments

- Built with Android WebView
- Uses AndroidX libraries
- Material Design components
