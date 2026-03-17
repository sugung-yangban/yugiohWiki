package net.datasa.Wiki.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datasa.Wiki.domain.dto.boardDTO;
import net.datasa.Wiki.domain.dto.boosterpackResponse;
import net.datasa.Wiki.domain.entity.*;
import net.datasa.Wiki.repository.BoardFileRepository;
import net.datasa.Wiki.repository.BoardRepository;
import net.datasa.Wiki.repository.RecommendRepository;
import net.datasa.Wiki.repository.wikiMemberRepository;
import net.datasa.Wiki.service.BoosterpackService;
import net.datasa.Wiki.service.boardService;
import net.datasa.Wiki.service.deckService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("board")
@Slf4j
@RequiredArgsConstructor
public class boardController {
	private final boardService boardService;
	private final BoosterpackService boosterpack;
	private final RecommendRepository recommendRepository;
	private final BoardRepository boardRepository;
	private final BoardFileRepository boardFileRepository;
	private final wikiMemberRepository wikiMemberRepository;
	private final deckService deckService;
	@Value("${board.upload.path}")
	private String uploadPath;
	@GetMapping("/download/{fileId}")
	public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) throws MalformedURLException {
		BoardFile fileEntity = boardFileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다"));
		
		String storeFileName = fileEntity.getSavedName();
		String uploadFileName = fileEntity.getOriginalName();
		
		UrlResource resource = new UrlResource("file:" + uploadPath + "/" + storeFileName);
		
		String encodeUploadFileName = UriUtils.encode(uploadFileName, StandardCharsets.UTF_8);
		String contentDisposition = "attachment; filename=\"" + encodeUploadFileName + "\"";
		
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
				.body(resource);
	}
	@PostMapping("/recommend/toggle")
	@ResponseBody
	public Map<String, Object> toggleRecommend(
			@RequestParam("boardId") Integer boardId,
			@RequestParam("type") Boolean type,
			Principal principal){
		Map<String,Object> response = new HashMap<>();
		
		if (principal==null){
			response.put("result","loginNeeded");
			return response;
		}
		
		String memberId = principal.getName();
		
		Optional<Recommend> existingOpt = recommendRepository.findByBoard_BoardIdAndMember_MemberId(boardId,memberId);
		if(existingOpt.isPresent()){
			Recommend recommend = existingOpt.get();
			
			if (recommend.getVoteType() == type){
				recommendRepository.delete(recommend);
				response.put("action","deleted");
			} else {
				recommend.setVoteType(type);
				recommendRepository.save(recommend);
				response.put("action","updated");
			}
		} else {
			board Board = boardRepository.findById(boardId).orElseThrow();
			wikiMember member = wikiMemberRepository.findById(memberId).orElseThrow();
			
			Recommend newRecommend = Recommend.builder()
					.board(Board)
					.member(member)
					.voteType(type)
					.build();
			recommendRepository.save(newRecommend);
			response.put("action","created");
		}
		response.put("likeCount", recommendRepository.countLike(boardId));
		response.put("dislikeCount",recommendRepository.countDislikes(boardId));
		response.put("result","success");
		
		return response;
	}
	@GetMapping("list")
	public String list(Model model, @RequestParam(value = "page", defaultValue = "0") int page){
		Page<boardDTO> boardList = boardService.getBoardList(page);
		model.addAttribute("boardList", boardList);
		return "board";
	}
	@GetMapping("write")
	public String write(Model model, Principal principal){
		String loginId = principal.getName();
		List<myDeck> myDecks = deckService.getMyDecks(loginId);
		model.addAttribute("myDecks", myDecks);
		model.addAttribute("loginUser",loginId);
		
		List<boosterpackResponse> cardList = boosterpack.selectAll("cardname","asc");
		model.addAttribute("cardList",cardList);
		return "write";
	}
	@PostMapping("write")
	public String write(@ModelAttribute boardDTO boardDTO, Model model, Principal principal){
		String loginId = principal.getName();
		model.addAttribute("loginUser",loginId);
		try {
			log.info("글쓰기 요청: {}", boardDTO);
			boardService.writeBoard(boardDTO, loginId);
		} catch (Exception e){
			e.printStackTrace();
			return "redirect:/board/write";
		}
		
		return "redirect:/board/list";
	}
	@GetMapping("read")
	public String read(@RequestParam("boardId") Integer boardId, Model model, Principal principal){
		int likeCount = recommendRepository.countLike(boardId);
		int dislikeCount = recommendRepository.countDislikes(boardId);
		model.addAttribute("likeCount",likeCount);
		model.addAttribute("dislikeCount",dislikeCount);
		
		if (principal != null){
			String loginId = principal.getName();
			model.addAttribute("loginUser",loginId);
			Optional<Recommend> myVote = recommendRepository.findByBoard_BoardIdAndMember_MemberId(boardId,loginId);
			
			if (myVote.isPresent()){
				model.addAttribute("userVote",myVote.get().getVoteType());
			} else {
				model.addAttribute("userVote",null);
			}
		} else {
				model.addAttribute("loginUser", null);
				model.addAttribute("userVote",null);
		}
		
		try{
			boardDTO boardDTO = boardService.getBoard(boardId);
			model.addAttribute("board",boardDTO);
			if (boardDTO.getDeckId() != null){
				try{
					List<boosterpackResponse> deckCards = deckService.getDeckCards(boardDTO.getDeckId());
					model.addAttribute("deckCards", deckCards);
					model.addAttribute("isDeckAlive",true);
				} catch (Exception e){
					System.out.println("연결된 덱이 사라졌습니다. " + e.getMessage());
					model.addAttribute("deckCards",null);
					model.addAttribute("isDeckAlive",false);
				}
			} else {
				model.addAttribute("isDeckAlive",true);
			}
			return "read";
		} catch (Exception e){
			e.printStackTrace();
			return "redirect:/board/list";
		}
	}
	@GetMapping("delete")
	public String delete(@RequestParam("boardId") Integer boardId, Principal principal, Model model){
		String loginId = principal.getName();
		model.addAttribute("loginUser",loginId);
		
		try {
			boardService.deleteBoard(boardId,loginId);
		} catch (Exception e){
			e.printStackTrace();
			return "redirect:/board/read?boardId=" + boardId;
		}
		return "redirect:/board/list";
	}
	@GetMapping("update")
	public String updateForm(@RequestParam("boardId") Integer boardId, Model model, Principal principal){
		String loginId = principal.getName();
		model.addAttribute("loginUser",loginId);
		boardDTO board = boardService.getBoard(boardId);
		
		if (!board.getWriter().equals(loginId)){
			return "redirect:/board/list";
		}
		List<boosterpackResponse> cardList = boosterpack.selectAll("cardname", "asc");
		
		model.addAttribute("cardList",cardList);
		model.addAttribute("board", board);
		return "update";
	}
	@PostMapping("update")
	public String update(@ModelAttribute boardDTO boardDTO, Model model, Principal principal){
		String loginId = principal.getName();
		model.addAttribute("loginUser",loginId);
		
		try{
			boardService.updateBoard(boardDTO, loginId);
			return "redirect:/board/read?boardId=" + boardDTO.getBoardId();
		} catch (Exception e){
			e.printStackTrace();
			return "redirect:/board/list";
		}
	}
	@DeleteMapping("/api/delete")
	@ResponseBody
	public String deleteBoardApi(@RequestParam("boardId")Integer boardId){
		try{
			String tempLoginId = "admin";
			boardService.deleteBoard(boardId,tempLoginId);
			return "게시글 삭제 성공 (boardId: " + boardId + " )";
		} catch (Exception e){
			return "삭제실패 : " + e.getMessage();
		}
	}
}
