package egovframework.backoffice.mvp.security;

import egovframework.backoffice.mvp.account.AccountMapper;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }

    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http, AccountDetailsService details,
                                                  PasswordEncoder encoder, AccountMapper accounts) throws Exception {
        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(details);
        provider.setPasswordEncoder(encoder);
        return http.authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/login", "/css/**", "/error").permitAll()
                        .requestMatchers("/admin/accounts/**").hasAuthority(AccessPolicy.Capability.MANAGE_ACCOUNTS.name())
                        .requestMatchers("/", "/account/password", "/admin/posts", "/admin/posts/**").authenticated()
                        .anyRequest().denyAll())
                .formLogin(form -> form.loginPage("/login").loginProcessingUrl("/login")
                        .failureUrl("/login?error").successHandler((request, response, authentication) -> {
                            var principal = (AccountPrincipal) authentication.getPrincipal();
                            response.sendRedirect(request.getContextPath() +
                                    (principal.isPasswordChangeRequired() ? "/account/password" : "/admin/posts"));
                        }).permitAll())
                .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true).clearAuthentication(true).deleteCookies("JSESSIONID"))
                .sessionManagement(session -> session.sessionFixation(fixation -> fixation.changeSessionId()))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .httpBasic(AbstractHttpConfigurer::disable)
                .headers(headers -> headers.contentSecurityPolicy(csp ->
                        csp.policyDirectives("default-src 'self'; style-src 'self'; form-action 'self'; frame-ancestors 'none'")))
                .addFilterBefore(new AccountSessionFilter(accounts), AuthorizationFilter.class)
                .build();
    }
}
