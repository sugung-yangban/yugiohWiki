package net.datasa.Wiki.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datasa.Wiki.Security.CustomUserDetails;
import net.datasa.Wiki.domain.dto.GameRoom;
import net.datasa.Wiki.domain.entity.myDeck;
import net.datasa.Wiki.repository.myDeckRepository;
import net.datasa.Wiki.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@Slf4j
@RequestMapping("/game")
@RequiredArgsConstructor
public class GameController {
	private final GameService gameService;
	private final myDeckRepository myDeckRepository;
	
	@GetMapping("/select")
	public String selectDeckPage(@AuthenticationPrincipal CustomUserDetails user, Model model){
		log.info("현재 로그인된 유저 객체 타입: {}", user.getClass().getName());
		String userId = user.getUsername();
		log.info("로그인 아이디: {}", userId);
		List<myDeck> myDecks = myDeckRepository.findByMember_MemberId(user.getUsername());
		model.addAttribute("myDecks", myDecks);
		return "game/deckSelect";
	}
	
	@GetMapping("/room")
	public String gameRoom(){
		return "game/room";
	}
	
	@ResponseBody
	@PostMapping("/start")
	public ResponseEntity<?>  startGame(
			@AuthenticationPrincipal UserDetails user,
			@RequestBody Map<String, Object> request
			){
		try{
			String userId = user.getUsername();
			Integer deckId = Integer.valueOf(request.get("deckId").toString());
			String difficulty = request.get("difficulty").toString();
			
			GameRoom room = gameService.createGame(userId, deckId,difficulty);
			return ResponseEntity.ok(room);
		} catch (Exception e){
			e.printStackTrace();
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	@ResponseBody
	@PostMapping("/rps")
	public ResponseEntity<?> playRps(@RequestBody Map<String, String> request){
		try{
			String roomId = request.get("roomId");
			String choice = request.get("choice");
			
			GameRoom room = gameService.processRps(roomId, choice);
			return ResponseEntity.ok(room);
		} catch (Exception e){
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@PostMapping("/summon")
	public ResponseEntity<?> summonMonster(@RequestBody Map<String, String> request){
		try{
			String roomId = request.get("roomId");
			String cardUniqueId = request.get("cardUniqueId");
			
			GameRoom room = gameService.summonMonster(roomId,cardUniqueId);
			return ResponseEntity.ok(room);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@PostMapping("/battle")
	public ResponseEntity<?> battle(@RequestBody Map<String, String> request){
		try {
			String roomId = request.get("roomId");
			
			GameRoom room = gameService.battle(roomId);
			return ResponseEntity.ok(room);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@PostMapping("/end-turn")
	public ResponseEntity<?> endTurn(@RequestBody Map<String, String> request){
		try{
			String roomId = request.get("roomId");
			
			GameRoom room = gameService.endTurn(roomId);
			return ResponseEntity.ok(room);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@GetMapping("/info")
	public ResponseEntity<?> getGameInfo(@RequestParam("roomId") String roomId){
		try{
			GameRoom room = gameService.getRoom(roomId);
			if (room == null){
				return ResponseEntity.badRequest().body("존재하지 않는 게임방입니다");
			}
			return ResponseEntity.ok(room);
		}catch (Exception e){
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@PostMapping("/surrender")
	public ResponseEntity<?> surrender(@RequestBody Map<String, String> request){
		try{
			String roomId = request.get("roomId");
			GameRoom room = gameService.surrender(roomId);
			return ResponseEntity.ok(room);
		} catch (Exception e){
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@PostMapping("/fusion")
	public ResponseEntity<?> fusionSummon(@RequestBody Map<String, Object> request){
		try {
			String roomId = (String) request.get("roomId");
			String targetId = (String) request.get("targetId");
			
			List<String> materialIds = (List<String>) request.get("materialIds");
			
			GameRoom room = gameService.fusionSummon(roomId, targetId, materialIds);
			return ResponseEntity.ok(room);
		} catch (Exception e){
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@GetMapping("fusion-materials")
	public ResponseEntity<?> getFusionMaterials(@RequestParam String roomId, @RequestParam String targetId){
		try{
			List<String> validIds = gameService.getValidFusionMaterials(roomId, targetId);
			return ResponseEntity.ok(validIds);
		} catch (Exception e){
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@GetMapping("summonable-fusions")
	public ResponseEntity<?> getSummonableFusions(@RequestParam String roomId){
		try{
			List<String> ids = gameService.getSummonableExtraDeckMonsters(roomId);
			return ResponseEntity.ok(ids);
		} catch (Exception e){
			return ResponseEntity.badRequest().body((e.getMessage()));
		}
	}
	
	@ResponseBody
	@PostMapping("discard")
	public ResponseEntity<?> discardCard(@RequestBody Map<String, String> request){
		try{
			String roomId = request.get("roomId");
			String cardUniqueId = request.get("cardUniqueId");
			GameRoom room = gameService.discardCard(roomId, cardUniqueId);
			return ResponseEntity.ok(room);
		} catch (Exception e){
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@PostMapping("/equip")
	public ResponseEntity<?> equipSpell(@RequestBody Map<String, String> request){
		try{
			String roomId = request.get("roomId");
			String spellUniqueId = request.get("spellUniqueId");
			String targetMonsterUniqueId = request.get("targetMonsterUniqueId");
			
			GameRoom room = gameService.equipSpell(roomId, spellUniqueId, targetMonsterUniqueId);
			return ResponseEntity.ok(room);
		} catch (Exception e){
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@ResponseBody
	@PostMapping("field-spell")
	public ResponseEntity<?> activateFieldSpell(@RequestBody Map<String, String> request){
		try{
			String roomId = request.get("roomId");
			String spellUniqueId = request.get("spellUniqueId");
			
			GameRoom room = gameService.activateFieldSpell(roomId,spellUniqueId);
			return ResponseEntity.ok(room);
		} catch (Exception e) {
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
}





























