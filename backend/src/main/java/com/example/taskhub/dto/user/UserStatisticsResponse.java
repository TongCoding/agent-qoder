package com.example.taskhub.dto.user;

import com.example.taskhub.enums.UserRole;
import com.example.taskhub.enums.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * 用户统计结果，用于前端仪表盘展示。
 *
 * @param total       用户总数
 * @param statusCounts 各状态数量，key 为状态枚举名
 * @param roleCounts  各角色数量，key 为角色枚举名
 * @author TaskHub
 */
@Schema(description = "用户统计")
public record UserStatisticsResponse(

        @Schema(description = "用户总数", example = "12")
        long total,

        @Schema(description = "各账号状态数量")
        Map<UserStatus, Long> statusCounts,

        @Schema(description = "各角色数量")
        Map<UserRole, Long> roleCounts
) {
}
