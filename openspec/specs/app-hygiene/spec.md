# app-hygiene Specification

## Purpose
TBD - created by archiving change hygiene. Update Purpose after archive.
## Requirements
### Requirement: Keep the app out of backups

The app SHALL NOT allow Android to back up or transfer its data: Auto Backup and adb backup SHALL be disabled, and on Android 12 and later both cloud backup and device-to-device transfer SHALL exclude every app data domain. A phone restored or set up from another one SHALL start the app signed out, with no session carried over.

#### Scenario: Package flags
- **WHEN** QA runs `adb shell dumpsys package com.qhathia.mobile.staff`
- **THEN** its flags do not include `ALLOW_BACKUP`

#### Scenario: Restored phone
- **WHEN** a phone is set up from a backup or a transfer of a phone where the merchant was signed in
- **THEN** the app opens on the login screen

### Requirement: Keep secrets out of the debug log

Release builds SHALL NOT log HTTP traffic. Debug builds MAY log requests and responses with their bodies, but SHALL replace the value of every password, token, exit PIN, phone and email field with `***`, and SHALL hide the `Authorization` and refresh-token headers. The log SHALL be written to logcat under the tag `KtorClient`.

#### Scenario: Signing in on a debug build
- **WHEN** the merchant signs in on a debug build
- **THEN** logcat shows the login request and response with `"password":"***"`, `"accessToken":"***"` and `"refreshToken":"***"`, and no `Bearer` header value

#### Scenario: Personal data on a debug build
- **WHEN** the merchant saves a phone in Datos personales on a debug build
- **THEN** logcat shows `"phone":"***"` and no phone number

#### Scenario: Fields that only look sensitive
- **WHEN** a response carries `passwordUpdatedAt` or `exitPinSet`
- **THEN** those values are logged as they are

