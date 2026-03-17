package net.datasa.Wiki.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import net.datasa.Wiki.Security.CustomUserDetails;
import net.datasa.Wiki.domain.dto.memberJoinDto;
import net.datasa.Wiki.domain.entity.wikiMember;
import net.datasa.Wiki.repository.wikiMemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Transactional
public class wikiMemberService implements UserDetailsService {

	private final wikiMemberRepository wikiMemberRepository;
	
	public void join(memberJoinDto dto){
		wikiMember entity = wikiMember.builder()
				.memberId(dto.getMemberId())
				.memberPassword(dto.getMemberPassword())
				.build();
		
		wikiMemberRepository.save(entity);
	}
	public boolean idCheck(String searchId){
		return wikiMemberRepository.existsById(searchId);
	}
	
	public wikiMember getMember(String id){
		return wikiMemberRepository.findById(id).orElse(null);
	}
	
	public void updateProfileImage(String memberId, String filename){
		wikiMember member = wikiMemberRepository.findById(memberId).orElseThrow(()->new RuntimeException("회원 없음"));
		member.setProfileImage(filename);
	}
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		wikiMember member = wikiMemberRepository.findById(username).orElseThrow(()->new UsernameNotFoundException("사용자를 찾을 수 없습니다"));
		
		return new CustomUserDetails(member);
	}
	
	public void deleteMember(String memberId) {
		wikiMember member = wikiMemberRepository.findById(memberId).orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));
		
		wikiMemberRepository.delete(member);
	}
}
