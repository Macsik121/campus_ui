Create a plan file at C:\Users\максим.claude\plans\notifications-section.md with the following content:

Plan: Notifications Section Implementation

Context

The Notifications section needs to be fully implemented according to the detailed requirements. The current implementation has basic structure but doesn't match the exact UI/UX specifications.

Current State Analysis

- NotificationFragment.java: Has basic RecyclerView setup, uses NotificationsApi directly (should use Repository pattern)
- NotificationAdapter.java: Basic adapter with OnNotificationActionListener, but needs proper card styling for read/unread states
- Layouts: fragment_notification.xml and item_notification.xml exist but need updates for exact styling
- Generated API: NotificationsApi has:
    - getNotifications(Integer limit) - GET /notifications?limit=20
    - readNotification(ReadNotificationRequest) - POST /notifications/read
    - readAllNotifications() - POST /notifications/read-all
- Models: Notification has id, title, description, sentAt (OffsetDateTime), isRead (Boolean)
- TimeUtils: Has relative time formatting but uses different format than required
- Icons: ic_notification_map, ic_notification_bell, ic_notification_important, ic_notification_info exist
- Colors: unread_background (#332196F3) exists

Required Changes

1. Create NotificationRepository.java

New file: app/src/main/java/com/sfedu/campus/notifications/NotificationRepository.java
- Follow UserRepository pattern
- Use ApiProvider.getApiClient(context) to get authenticated client
- Methods:
    - getNotifications(DataCallback<List<Notification>> callback) - GET /notifications?limit=20
    - markAsRead(UUID notificationId, DataCallback<ReadNotificationResponse> callback) - POST /notifications/read
    - markAllAsRead(DataCallback<ReadAllNotifications200Response> callback) - POST /notifications/read-all

2. Update NotificationFragment.java

- Use NotificationRepository instead of direct API calls
- Implement exact UI requirements:
    - Header: "Страница - Отображаются уведомления"
    - Unread count display with format "Непрочитано: X"
    - "Прочитать всё" button visibility based on unread count
    - Loading overlay/freeze UI during requests
- Implement freeze/unfreeze logic:
    - On markAsRead: freeze card/button, unfreeze after response, update single card + count
    - On markAllAsRead: freeze all, unfreeze after, update all cards + count = 0, hide button
- Update unread count in real-time

3. Update NotificationAdapter.java

- Card styling per requirements:
    - Unread: transparent blue background (#332196F3) + thicker blue left border + rounded corners + thin black border
    - Read: white background + no left border + rounded corners + thin black border
- Notification type icons:
    - Map marker (ic_notification_map) for "перемещения по карте"
    - Bell (ic_notification_bell) for "информативное объявление"
    - Red exclamation (ic_notification_important) for "важное объявление"
    - Info (ic_notification_info) as fallback
- Time display in bottom-right using new format
- Bind "Прочитано" button per card, callback to listener

4. Update item_notification.xml

- Add left border view for unread state
- Add notification type icon ImageView
- Add "Прочитано" button per card
- Time TextView in bottom-right
- Proper card styling with rounded corners, thin black border

5. Update fragment_notification.xml

- Add page title "Страница - Отображаются уведомления"
- Ensure header layout matches requirements

6. Add/Update TimeUtils

- Add new method getNotificationRelativeTime(OffsetDateTime sentAt) with exact required format

7. Update strings.xml if needed

- Add any missing strings for notification types, button texts, etc.

Verification

- Build project: ./gradlew assembleDebug
- Run on device/emulator
- Test navigation to Notifications tab
- Test mark single notification as read
- Test mark all as read
- Verify UI states (read/unread styling, icons, time format)
- Verify freeze/unfreeze during API calls