rootProject.name = "banking-app"

// notification-service: its own Spring Boot app in services/notification-service, built with the same wrapper.
//   ./gradlew build                                -> both apps, all tests
//   ./gradlew :notification-service:bootRun        -> only the service
include("notification-service")
project(":notification-service").projectDir = file("services/notification-service")
