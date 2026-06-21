package com.bp.jaringochi.domain.report.dto;

import lombok.Getter;
import lombok.Setter;

/** "굴비에게 한 마디" 요청 바디 */
@Getter
@Setter
public class TalkRequest {
    private Integer year;
    private Integer month;
    private String message;
}
