package com.spring2025.vietchefs.models.payload.responseModel;

import lombok.Data;

import java.time.LocalTime;

@Data
public class TimeTravelResponse {
    private LocalTime timeBeginTravel;
    private LocalTime timeBeginCook;
}
