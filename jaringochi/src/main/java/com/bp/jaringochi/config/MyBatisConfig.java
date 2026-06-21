package com.bp.jaringochi.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan({
        "com.bp.jaringochi.domain.budget.dao",
        "com.bp.jaringochi.domain.category.dao", 
        "com.bp.jaringochi.domain.notification.dao",
        "com.bp.jaringochi.domain.report.dao",
        "com.bp.jaringochi.domain.statistics.dao",
        "com.bp.jaringochi.domain.transaction.dao",
        "com.bp.jaringochi.domain.user.dao",
        "com.bp.jaringochi.domain.gulbi.dao"
})
public class MyBatisConfig {
}