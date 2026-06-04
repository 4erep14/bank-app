// Story: US-016
package com.northbank.registration.notification.exception;

public class NotificationAccessDeniedException extends RuntimeException {
    public NotificationAccessDeniedException() {
        super("Notifications can only be retrieved for the authenticated customer");
    }
}
