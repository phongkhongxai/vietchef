package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationCountResponse {
    private Integer allNoti;
    private Integer chatNoti;
    private Integer notiNotChat;
}
