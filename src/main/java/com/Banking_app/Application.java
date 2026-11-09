package com.Banking_app;

import com.Banking_app.fx.adapter.out.config.FxRatesProperties;
import com.Banking_app.security.LoginThrottleProperties;
import com.Banking_app.security.refresh.RefreshTokenProperties;
import com.Banking_app.userProfile.adapter.in.startup.BootstrapAdminProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
@SpringBootApplication
@EnableConfigurationProperties({BootstrapAdminProperties.class, LoginThrottleProperties.class, RefreshTokenProperties.class,
		FxRatesProperties.class})
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
