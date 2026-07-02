package pe.prismadev.servmedic.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import pe.prismadev.servmedic.security.JwtAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) ->
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No autenticado.")
                )
                .accessDeniedHandler((request, response, accessDeniedException) ->
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acceso denegado.")
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                .requestMatchers("/api/public/auth/admin/login").permitAll()
                .requestMatchers("/api/public/auth/patient/login").permitAll()
                .requestMatchers("/api/public/auth/specialist/login").permitAll()

                .requestMatchers("/api/public/health").permitAll()
                .requestMatchers("/api/public/catalog/**").permitAll()
                .requestMatchers("/api/public/register/**").permitAll()

                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/patient/**").hasRole("PACIENTE")
                .requestMatchers("/api/specialist/**").hasRole("ESPECIALISTA")
                .requestMatchers("/api/dev/**").hasRole("ADMIN")

                .requestMatchers("/api/public/admin/**").denyAll()
                .requestMatchers("/api/public/medical-requests/**").denyAll()
                .requestMatchers("/api/public/patients/**").denyAll()
                .requestMatchers("/api/public/specialists/**").denyAll()

                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/info").permitAll()
                .requestMatchers("/error").permitAll()

                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*"
        ));

        configuration.setAllowedMethods(List.of(
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
        ));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setExposedHeaders(List.of(
            "Authorization",
            "Content-Type"
        ));

        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}