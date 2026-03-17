package net.datasa.Wiki.Controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datasa.Wiki.domain.dto.memberJoinDto;
import net.datasa.Wiki.domain.entity.myDeck;
import net.datasa.Wiki.domain.entity.wikiMember;
import net.datasa.Wiki.service.deckService;
import net.datasa.Wiki.service.wikiMemberService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.List;

@Controller
@Slf4j
@RequestMapping("user")
@RequiredArgsConstructor
public class loginController {
	private final wikiMemberService wikiMemberService;
	private String uploadDir = "C:/wiki_upload/";
	private final deckService deckService;
	@GetMapping("mypage")
	public String mypage(Principal principal, Model model){
		
		if (principal == null){
			return "redirect:/user/login";
		}
		
		String loginId = principal.getName();
		model.addAttribute("loginUser",loginId);
		
		wikiMember Wikimember = wikiMemberService.getMember(loginId);
		model.addAttribute("wikimember",Wikimember);
		
		List<myDeck> myDeckList	= deckService.getMyDecks(loginId);
		model.addAttribute("myDeckList",myDeckList);
		
		return "mypage";
	}
	@PostMapping("/updateProfile")
	public String updateProfile(HttpSession session, @RequestParam("upload") MultipartFile upload){
		String loginId = (String) session.getAttribute("loginId");
		if (loginId == null) return "redirect:/user/login";
		
		if(upload.isEmpty()){
			return "redirect:/user/mypage";
		}
		
		try{
			String originalFilename = upload.getOriginalFilename();
			String savedFilename = loginId + "_" + originalFilename;
			File file = new File(uploadDir + savedFilename);
			upload.transferTo(file);
			wikiMemberService.updateProfileImage(loginId,savedFilename);
		} catch (IOException e){
			e.printStackTrace();
		}
		
		return "redirect:/user/mypage";
	}
	@GetMapping("login")
	public String loginform(){
		return "login";
	}
	@GetMapping("logout")
	public String logout(HttpSession session){
		session.invalidate();
		return "redirect:/";
	}
	@GetMapping("register")
	public String register(){
		return "registerForm";
	}
	@PostMapping("register")
	public String registersubmit(@ModelAttribute memberJoinDto memberJoinDto){
		wikiMemberService.join(memberJoinDto);
		return "redirect:/";
	}
	@GetMapping("/idCheck")
	public String idCheck(){
		return "idCheck";
	}
	@PostMapping("idCheck")
	public String idCheckProcess(@RequestParam("searchId") String searchId, Model model){
		boolean isDuplicate = wikiMemberService.idCheck(searchId);
		model.addAttribute("searchId",searchId);
		model.addAttribute("isDuplicate", isDuplicate);
		model.addAttribute("checked", true);
		
		return "idCheck";
	}
	@PostMapping("/delete")
	@ResponseBody
	public ResponseEntity<String> deleteDeck(@RequestParam("deckId") Integer deckId, Principal principal,Model model){
		String loginId = principal.getName();
		model.addAttribute("loginUser",loginId);
		if (loginId == null){
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다");
		}
		try{
			deckService.deletedeck(deckId,loginId);
			return ResponseEntity.ok("삭제되었습니다");
		} catch (Exception e){
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("삭제실패: " + e.getMessage());
		}
	}
	@PostMapping("/deleteMember")
	public String deleteMember(Principal principal, HttpSession session){
		if (principal == null){
			return "redirect:/user/login";
		}
		String loginId = principal.getName();
		wikiMemberService.deleteMember(loginId);
		session.invalidate();
		org.springframework.security.core.context.SecurityContextHolder.clearContext();
		return "redirect:/";
	}
}
