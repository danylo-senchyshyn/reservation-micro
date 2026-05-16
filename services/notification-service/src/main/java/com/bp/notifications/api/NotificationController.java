package com.bp.notifications.api;

import com.bp.notifications.entity.NotificationLog;
import com.bp.notifications.repository.NotificationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationLogRepository notificationLogRepository;

    @GetMapping
    public List<NotificationLog> getAll() {
        return notificationLogRepository.findAll();
    }
}