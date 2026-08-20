# Архитектура пользовательского интерфейса (UI)
Используемые технологии: Java, XML, OkHttp, JWT.

Проект построен на архитектуре с разделением на слои (UI, Data, Utils) - Feature-based organization.
Взаимодействие происходит по следующей схеме:
- **UI Layer** (папка `ui`): Содержит экраны (Activities) и логику отображения (Fragments).
    - `auth` — модуль авторизации (Login/Register).
    - `squad` — управление списком детей.
    - `notifications` - просмотр последних уведомлений
    - `profile` — профиль пользователя.
- **Data Layer** (папка `models`): POJO-классы для передачи данных между сервером и клиентом (LoginResponse, Child, Notification и т.д.).
- **Network & Utility Layer** (папка `utils`):
    - `ApiClient` — инкапсулирует OkHttp, настраивает заголовки и выполняет запросы.
    - `PreferencesHelper` — абстракция над SharedPreferences для безопасной работы с JWT. декодировать токен можно с помощью метода decodePayload.
- Зависимости и сборка: Gradle-зависимости: OkHttp, Gson, Swagger.

## Главные активности

## 1. Экран авторизации (AuthActivity)
- **Роль:** Точка входа, если пользователь не авторизован.
- **Контейнер:** `AuthActivity`.
- **Вложенные компоненты:**
  - `LoginFragment` (форма входа).
  - `RegisterFragment` (форма регистрации).
- **Логика переключения:** Кнопки "Вход" и "Регистрация" переключают фрагменты внутри `AuthActivity`.
- **Логика работы:** После успешного "Войти"(LoginFragment) или "Зарегестрироваться"(RegisterFragment) в `SharedPreferences` записывается токен и переключается на `MainActivity`.

## 2. Главный экран (MainActivity)
- **Роль:** Основной контейнер для авторизованного пользователя.
- **Контейнер:** `MainActivity`.
- **Нижняя навигация (BottomNavigationView):** Содержит вкладки в зависимости от прав вожатого.
- **Вкладка "Отряд" (`SquadFragment`):** `RecyclerView` со списком детей. Карточка ребенка содержит теги.
- **Вкладка "Уведомления" (`NotificationsFragment`):** Список уведомлений. Кнопка «Прочитать все».
- **Вкладка "Карта" (`MapFragment`):** (Если сделаете) GoogleMaps/OSM с маркерами объектов.
- **Вкладка "Журнал" (`DetachmentLogFragment`):** список активностей(мероприятий) и детей, присутствующих/отсутствующих на них
- **Вкладка "Профиль" (`ProfileFragment`):** Отображает данные пользователя, кнопка "Выйти".
- **Логика работы:** Если токен не присутствует в `SharedPreferences`, то активность переключаетс на `AuthActivity`.

## 3. Навигационные переходы
- Переход `AuthActivity` → `MainActivity`: происходит после успешного входа. Использует `FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK`.
- Переход `MainActivity` → `AuthActivity`: происходит по кнопке "Выйти". Использует `FLAG_ACTIVITY_NEW_TASK | CLEAR_TASK`.

Mermaid-диаграмма:
graph TD
%% 1. Слой UI: Навигация между экранами
subgraph AuthFlow["UI: Экран авторизации"]
Auth["AuthActivity"] --> Login["LoginFragment"]
Auth --> Register["RegisterFragment"]
end
    subgraph MainFlow["UI: Главный экран"]
        Main["MainActivity"] --> BottomNav["BottomNavigationView"]
        BottomNav --> Squad["SquadFragment"]
        BottomNav --> Notif["NotificationsFragment"]
        BottomNav --> Map["MapFragment"]
        BottomNav --> DetachLog["DetachmentLogFragment"]
        BottomNav --> Profile["ProfileFragment"]
    end

    %% Переходы между экранами
    Login -- "Успешный вход/регистрация" --> Main
    Profile -- "Кнопка 'Выйти' (очистка SharedPreferences)" --> Auth

    %% 2. Слой Вспомогательных классов (Helpers)
    subgraph Utils["Слой Утилит и данных"]
        ApiClient["ApiClient (OkHttp)"]
        Prefs["PreferencesHelper (SharedPreferences)"]
    end

    %% 3. Слой Моделей (DTO)
    subgraph Models["Модели данных"]
        LoginResp["LoginResponse"]
        RegistResp["RegisterResponse"]
        Child["Child"]
        UserProf["UserProfile"]
    end

    %% Взаимодействие UI -> Helpers
    Login -.->|"POST /auth/register"| ApiClient
    Register -.->|"POST /auth/login"| ApiClient
    Squad -.->|"GET /children"| ApiClient
    Notif -.->|"GET /notifications"| ApiClient
    Profile -.->|"GET /profile"| ApiClient
    Map -.->|"GET /map"| ApiClient
    DetachLog -.->|"GET /events"| ApiClient
    Main -.->|"Чтение токена(decodePayload methodf)"| Prefs
    Main -.->|"Валидация токена не сервере"| ApiClient

    %% Взаимодействие Helpers -> Models (Превращение JSON в объекты)
    ApiClient -.->|"Сериализация (Gson)"| Models

    %% Взаимодействие Helpers -> Другие Helpers
    Login -.->|"Сохранить JWT"| Prefs
    Register -.->|"Сохранить JWT"| Prefs
    Profile -.->|"Очистить JWT"| Prefs
    Prefs -.->
    ApiClient -.->|"Добавить JWT в заголовки"| Prefs
