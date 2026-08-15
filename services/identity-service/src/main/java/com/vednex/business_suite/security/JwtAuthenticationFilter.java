package com.vednex.business_suite.security;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import com.vednex.business_suite.common.exception.InvalidTokenException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	private final JwtService jwtService;

	public JwtAuthenticationFilter(JwtService jwtService) {
		this.jwtService = jwtService;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String token = bearerToken(request);
		if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
			try {
				JwtPrincipal principal = jwtService.parseAccessToken(token);
				List<SimpleGrantedAuthority> authorities = Stream.concat(
								principal.roles().stream().map(role -> "ROLE_" + role),
								principal.permissions().stream().map(permission -> "PERMISSION_" + permission)
						)
						.map(SimpleGrantedAuthority::new)
						.toList();
				UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(principal, token, authorities);
				SecurityContextHolder.getContext().setAuthentication(authentication);
			}
			catch (InvalidTokenException ex) {
				SecurityContextHolder.clearContext();
			}
		}
		filterChain.doFilter(request, response);
	}

	private String bearerToken(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			return null;
		}
		String token = authorization.substring(7).trim();
		return token.isBlank() ? null : token;
	}

}
