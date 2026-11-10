package com.opticsappbg.opticsapp.config;

import com.opticsappbg.opticsapp.security.ActiveUserSessionFilter;
import com.opticsappbg.opticsapp.user.repository.UserRepository;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableMethodSecurity
public class WebMvcConfiguration implements WebMvcConfigurer {


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            UserRepository userRepository) throws Exception {

        http
                .authorizeHttpRequests(matchers -> matchers
                        .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                        .requestMatchers("/", "/login").permitAll()
                        .requestMatchers("/deliveries/**", "/suppliers/**", "/users/**").hasAnyRole("ADMIN", "OWNER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .defaultSuccessUrl("/home", true)
                        .failureUrl("/login?error")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                )
                .addFilterBefore(
                        new ActiveUserSessionFilter(userRepository),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

}
