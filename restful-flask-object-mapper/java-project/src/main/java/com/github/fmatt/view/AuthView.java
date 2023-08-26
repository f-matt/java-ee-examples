package com.github.fmatt.view;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@Named
@RequestScoped
public class AuthView {

    private static final Logger logger = Logger.getLogger("AuthView");

	@Inject
	private SessionDataView sessionDataView;

	@Inject
	private FacesContext facesContext;
	
	private String username;
	
	private String password;

	public void createToken() {
		try {
			sessionDataView.setAuthenticated(false);

			Random random = new Random();
			String jwtId = "";
			for (int i = 0; i < 32; ++i)
				jwtId += Integer.toHexString(random.nextInt(15));

			Algorithm algorithm = Algorithm.HMAC256("abcdefghijklmnopkrstuvwxyz");
			String token = JWT.create()
				.withExpiresAt(LocalDateTime.now().plusMinutes(1).atZone(ZoneId.systemDefault()).toInstant())
				.withIssuedAt(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
				.withNotBefore(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
				.withJWTId(jwtId)
				.withClaim("sub", "user")
				.withClaim("type", "access")
				.sign(algorithm);

			sessionDataView.setToken(token);
			sessionDataView.setAuthenticated(true);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
			FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error creating access token.", null));
		}
	}
		
	public String login() {
		HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
		sessionDataView.setAuthenticated(false);

		try {
			request.login(username, password);
			createToken();
			return "/index.jsf?faces-redirect=true";
		} catch(ServletException e) {
            FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Invalid username/password", null));
			return "/login.jsf";
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.getMessage(), e);
            FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unknown error on login", null));
			return "login.jsf";
		}
	}
		
	public String logout() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		HttpSession session = request.getSession();
		
		try {
			request.logout();

			if (session != null)
				session.invalidate();

			return "/login.jsf?faces-redirect=true&logout=true";
		} catch (ServletException e) {
            FacesContext.getCurrentInstance().addMessage(null, 
				new FacesMessage(FacesMessage.SEVERITY_ERROR, "Unknown error on logout", null));
			return "/login.jsf?faces-redirect=true&error=true";
		}
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
