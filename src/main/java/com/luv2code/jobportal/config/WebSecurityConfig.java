package com.luv2code.jobportal.config;

import com.luv2code.jobportal.services.CustomUserDetailsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class WebSecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    private final CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler;

    @Autowired
    public WebSecurityConfig(CustomUserDetailsService customUserDetailsService, CustomAuthenticationSuccessHandler customAuthenticationSuccessHandler) {
        this.customUserDetailsService = customUserDetailsService;
        this.customAuthenticationSuccessHandler = customAuthenticationSuccessHandler;
    }


    // below here these are URLs that Spring Security will not provide protection support i.e. authentication is not required for accessing the home page, login or register page like that.
    private final String[] publicUrl = {"/",
            "/global-search/**",
            "/register",
            "/register/**",
            "/webjars/**",
            "/resources/**",
            "/assets/**",
            "/css/**",
            "/summernote/**",
            "/js/**",
            "/*.css",
            "/*.js",
            "/*.js.map",
            "/fonts**", "/favicon.ico", "/resources/**", "/error"};



    @Bean
    protected SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.authenticationProvider(authenticationProvider());

        // here below code means anyone can access the API endpoints directly without having authentication
        http.authorizeHttpRequests(auth-> {
                auth.requestMatchers(publicUrl).permitAll();
                auth.anyRequest().authenticated();   // here PLEASE NOTE: here we're protecting the urls like"/dashboard/" so we cannot access the /dashboard/ API url directly. We need to first login and Spring security will check the user credentials authentication.  any protected API endpoint request has to be authenticated or user has to give username and password to log in except the publicUrl.
        });

        //configuring custom login page our success handler and logout functionality
        http.formLogin(form -> form.loginPage("/login").permitAll()
                .successHandler(customAuthenticationSuccessHandler))
            .logout(logout -> {
                logout.logoutUrl("/logout");
                logout.logoutSuccessUrl("/");
            })
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable());

        return http.build();
    }

    // below method is our custom authentication provider i.e. tells spring security how to find our users and how to authenticate password
    @Bean
    public AuthenticationProvider authenticationProvider() {

        DaoAuthenticationProvider authenticationProvider = new DaoAuthenticationProvider();

        // tell spring security how to retrieve the users password from the database. here password would be in encrypted form
        authenticationProvider.setPasswordEncoder(passwordEncoder());

        // tell spring security how to retrieve the users email from the database
        authenticationProvider.setUserDetailsService(customUserDetailsService);

        return authenticationProvider;

    }

    // below method is our custom password encoder. i.e. tells spring security how to authenticate passwords (either should we authenticate using plain text or use encryption.
    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }
}
