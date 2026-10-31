package com.zuche.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置
 *
 * <p>访问地址：http://localhost:8080/doc.html
 * 右上角"Authorize"填入登录返回的 token（不需要手动加 Bearer 前缀）。</p>
 */
@Configuration
public class Knife4jConfig {

    private static final String SECURITY_SCHEME = "Authorization";

    @Bean
    public OpenAPI zucheOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("微租车平台 API")
                        .version("v1.0.0")
                        .description("""
                                基于信用风控的微租车平台后端接口。

                                核心链路：车辆浏览 → 下单（按信用等级计算动态押金）→ 在线支付 →
                                取车 → 归还 → 押金退还 → 订单完成 → 信用分自动更新。
                                """)
                        .contact(new Contact().name("zuche")))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name(SECURITY_SCHEME)))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME));
    }
}
