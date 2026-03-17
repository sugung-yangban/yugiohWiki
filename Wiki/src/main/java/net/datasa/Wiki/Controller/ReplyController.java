package net.datasa.Wiki.Controller;

import lombok.RequiredArgsConstructor;
import net.datasa.Wiki.domain.dto.ReplyDTO;
import net.datasa.Wiki.domain.entity.Reply;
import net.datasa.Wiki.domain.entity.board;
import net.datasa.Wiki.domain.entity.wikiMember;
import net.datasa.Wiki.repository.BoardRepository;
import net.datasa.Wiki.repository.ReplyRepository;
import net.datasa.Wiki.repository.wikiMemberRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reply")
public class ReplyController {
	private final ReplyRepository replyRepository;
	private final BoardRepository boardRepository;
	private final wikiMemberRepository memberRepository;
	
	@PostMapping("/save")
	public String saveReply(@RequestBody ReplyDTO replyDTO, @AuthenticationPrincipal UserDetails userDetails){
		if (userDetails == null) return "fail";
		
		board boardinfo = boardRepository.findById(replyDTO.getBoardId()).orElse(null);
		wikiMember member = memberRepository.findById(userDetails.getUsername()).orElse(null);
		
		if (boardinfo != null && member !=null){
			Reply reply = Reply.builder()
					.board(boardinfo)
					.member(member)
					.text(replyDTO.getText())
					.build();
			replyRepository.save(reply);
			return "success";
		}
		return "fail";
	}
	@GetMapping("/list")
	public List<ReplyDTO> getReplyList(@RequestParam("boardId") Integer boardId){
		List<Reply> replyList = replyRepository.findByBoard_BoardIdOrderByCreatedDateAsc(boardId);
		
		List<ReplyDTO> dtoList = new ArrayList<>();
		for (Reply r : replyList) {
			dtoList.add(ReplyDTO.builder()
							.replyId(r.getReplyId())
							.boardId(r.getBoard().getBoardId())
							.memberId(r.getMember().getMemberId())
							.text(r.getText())
							.createdDate(r.getCreatedDate())
							.build());
		}
		return dtoList;
	}
	@PostMapping("/delete")
	public String deleteReply(@RequestParam("replyId") Integer replyId, @AuthenticationPrincipal UserDetails userDetails) {
		Reply reply = replyRepository.findById(replyId).orElse(null);
		if (reply != null && reply.getMember().getMemberId().equals(userDetails.getUsername())){
			replyRepository.delete(reply);
			return "success";
		}
		return "fail";
	}
	private ReplyDTO convertToDTO(Reply reply){
		ReplyDTO dto = ReplyDTO.builder()
				.replyId(reply.getReplyId())
				.boardId(reply.getBoard().getBoardId())
				.memberId(reply.getMember().getMemberId())
				.text(reply.getText())
				.createdDate(reply.getCreatedDate())
				.parentId(reply.getParentReply() != null ? reply.getParentReply().getReplyId() : null)
				.childReplies(new ArrayList<>())
				.build();
		if (reply.getChildReplies() != null && !reply.getChildReplies().isEmpty()){
			for (Reply child : reply.getChildReplies()){
				dto.getChildReplies().add(convertToDTO(child));
			}
		}
		return dto;
	}
}
