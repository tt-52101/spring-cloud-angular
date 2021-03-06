package ma.fgs.product.security;

import static ma.fgs.product.security.utils.SecurityConstants.EXPIRATION_TIME;
import static ma.fgs.product.security.utils.SecurityConstants.HEADER_STRING;
import static ma.fgs.product.security.utils.SecurityConstants.SECRET;
import static ma.fgs.product.security.utils.SecurityConstants.TOKEN_PREFIX;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.google.gson.Gson;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import ma.fgs.product.domain.Account;
import ma.fgs.product.service.api.IUserService;
import ma.fgs.product.service.exception.NotFoundException;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

	private IUserService userService;

	public JwtAuthenticationFilter(IUserService userService) {
		this.userService = userService;
	}

	@Override
	public Authentication attemptAuthentication(HttpServletRequest req, HttpServletResponse res)
			throws AuthenticationException {
		try {
			if (req.getMethod().equalsIgnoreCase("OPTIONS"))
				return null;

			Account creds = new ObjectMapper().readValue(req.getInputStream(), Account.class);

			return getAuthenticationManager().authenticate(new UsernamePasswordAuthenticationToken(creds.getUsername(),
					creds.getPassword(), Collections.emptyList()));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void successfulAuthentication(HttpServletRequest req, HttpServletResponse res, FilterChain chain,
			Authentication auth) throws IOException, ServletException {
		Gson gson = new Gson();
		String username = ((User) auth.getPrincipal()).getUsername();
		final Claims claims = Jwts.claims().setSubject(username);
		if (((User) auth.getPrincipal()).getAuthorities() != null
				&& !((User) auth.getPrincipal()).getAuthorities().isEmpty()) {
			claims.put("role",
					((User) auth.getPrincipal()).getAuthorities().iterator().next().getAuthority().toUpperCase());
		}
		String token = Jwts.builder().setClaims(claims)
				.setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
				.signWith(SignatureAlgorithm.HS512, SECRET.getBytes()).compact();

		ma.fgs.product.domain.User user = null;
		try {
			user = userService.findUserByUsername(username);
			user.setAccessToken(token);
			res.getWriter().write(gson.toJson(user));
			res.getWriter().flush();
			res.getWriter().close();
			res.addHeader(HEADER_STRING, TOKEN_PREFIX + token);
		} catch (NotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
