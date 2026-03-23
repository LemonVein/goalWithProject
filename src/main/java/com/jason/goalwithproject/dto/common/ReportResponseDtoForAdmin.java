package com.jason.goalwithproject.dto.common;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponseDtoForAdmin {
    private Long id;
    private String reason;
    private Long reporterId;
    private Long targetId;
}
