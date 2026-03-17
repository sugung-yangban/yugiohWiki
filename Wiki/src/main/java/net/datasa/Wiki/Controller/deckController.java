package net.datasa.Wiki.Controller;

import lombok.RequiredArgsConstructor;
import net.datasa.Wiki.domain.dto.DeckSaveDTO;
import net.datasa.Wiki.domain.dto.boosterpackResponse;
import net.datasa.Wiki.service.BoosterpackService;
import net.datasa.Wiki.service.deckService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("deck")
@RequiredArgsConstructor
public class deckController {
	private final BoosterpackService boosterpackService;
	private final deckService deckService;
	
	@GetMapping("/build")
	public String makedeck(Principal principal, Model model,
						   @RequestParam(value="page",defaultValue = "0")int page,
						   @RequestParam(value = "keyword", required = false) String keyword,
						   @RequestParam(value = "race", required = false) String race,
						   @RequestParam(value = "element", required = false) String element,
						   @RequestParam(value = "level", required = false) String level,
						   @RequestParam(value = "sort", required = false) String sort,
						   @RequestParam(value = "deckId", required = false)Integer deckId,
						   @RequestParam(value = "cardtype", required = false)String cardtype,
						   @RequestParam(value = "type", required = false)String type){
		
		if (principal == null) {
			return "redirect:/user/login";
		}
		if(deckId != null){
			List<boosterpackResponse> loadedDeck = deckService.getDeckCards(deckId);
			model.addAttribute("loadedDeck",loadedDeck);
		}
		String loginId = principal.getName();
		model.addAttribute("loginUser",loginId);
		Page<boosterpackResponse> paging = boosterpackService.getPaging(page, keyword, race, element,level, sort, cardtype, type);
		model.addAttribute("paging",paging);
		model.addAttribute("keyword",keyword);
		model.addAttribute("race", race);
		model.addAttribute("element", element);
		model.addAttribute("level",level);
		model.addAttribute("sort", sort);
		model.addAttribute("cardtype",cardtype);
		model.addAttribute("type",type);
		
		return "deck/build";
	}
	
	
	@PostMapping("/save")
	@ResponseBody
	public ResponseEntity<String> saveDeck(@RequestBody DeckSaveDTO deckDTO, Principal principal, Model model){
		String loginId = principal.getName();
		model.addAttribute("loginUser",loginId);
		try{
			deckService.createDeck(loginId, deckDTO);
			return ResponseEntity.ok("저장되었습니다");
		} catch (Exception e){
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("저장 실패: " + e.getMessage());
		}
	}
	
}
