package net.datasa.Wiki.Security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(auth -> auth.requestMatchers("/","/user/**",
								"/img/**","/js/**","/error","/css/**","/board/list","/board/read", "/showme/**")
						.permitAll()
						.anyRequest().authenticated())
				.formLogin(login -> login
						.loginPage("/user/login")
						.loginProcessingUrl("/user/login")
						.usernameParameter("id")
						.passwordParameter("pw")
						.defaultSuccessUrl("/",true)
						.permitAll()
						
				)
				.logout(logout -> logout
						.logoutUrl("/user/logout")
						.logoutSuccessUrl("/")
						.invalidateHttpSession(true)
				);
				return http.build();
	}
	@Bean
	public PasswordEncoder passwordEncoder(){
		return org.springframework.security.crypto.password.NoOpPasswordEncoder.getInstance();	}
}
