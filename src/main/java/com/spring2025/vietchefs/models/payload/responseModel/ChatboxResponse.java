package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@AllArgsConstructor
public class ChatboxResponse {
    private String reply;
    private boolean suggestContactAdmin;

}
