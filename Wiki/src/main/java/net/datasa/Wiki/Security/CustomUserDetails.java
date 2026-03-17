package net.datasa.Wiki.Security;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.datasa.Wiki.domain.entity.wikiMember;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

	private final wikiMember member;
	
	@Override
	public String getUsername(){
		return member.getMemberId();
	}
	
	@Override
	public String getPassword(){
		return member.getMemberPassword();
	}
	
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities(){
		return Collections.singletonList(new SimpleGrantedAuthority("Role_User"));
	}
	
	@Override
	public boolean isAccountNonExpired(){
		return true;
	}
	
	@Override
	public boolean isAccountNonLocked(){
		return true;
	}
	
	@Override
	public boolean isCredentialsNonExpired(){
		return true;
	}
	
	@Override
	public boolean isEnabled(){
		return true;
	}
}
