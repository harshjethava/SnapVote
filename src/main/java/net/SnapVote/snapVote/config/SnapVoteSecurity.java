package net.SnapVote.snapVote.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SnapVoteSecurity {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/public/dashboard").permitAll()  //  Secure dashboard
                        .requestMatchers("/public/register", "/public/login", "/public/success").permitAll()
                        .requestMatchers("/user/**").permitAll()
                        .requestMatchers("/admin/create-admin","/admin/admin-login").permitAll()
                        .requestMatchers("/admin/**").permitAll()

                        .anyRequest().permitAll()
                )
//                .formLogin(form -> form
//                        .loginPage("/public/login")
//                        .defaultSuccessUrl("/public/dashboard", true)
//                        .permitAll()
//                )
                .formLogin(form -> form.disable())
                .logout(logout -> logout
                        .logoutSuccessUrl("/public/login")
                        .permitAll()
                );

        return http.build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
