package net.datasa.Wiki.service;

import net.datasa.Wiki.repository.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datasa.Wiki.domain.dto.boardDTO;
import net.datasa.Wiki.domain.entity.BoardFile;
import net.datasa.Wiki.domain.entity.board;
import net.datasa.Wiki.domain.entity.wikiMember;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class boardService {
	
	private final BoardRepository boardRepository;
	private final wikiMemberRepository wikiMemberRepository;
	private final BoardFileRepository boardFileRepository;
	private final RecommendRepository recommendRepository;
	private final ReplyRepository replyRepository;
	
	@Value("${board.upload.path}")
	private String uploadPath;
	
	public void writeBoard(boardDTO boardDTO, String loginId) throws IOException {
		wikiMember member = wikiMemberRepository.findById(loginId).orElseThrow(() -> new RuntimeException("회원 정보를 찾을 수 없습니다."));
	
		board Board = board.builder()
				.title(boardDTO.getTitle())
				.content(boardDTO.getContent())
				.member(member)
				.viewCount(0)
				.deckId(boardDTO.getDeckId())
				.build();
		
		board savedBoard = boardRepository.save(Board);
		
		List<MultipartFile> files = boardDTO.getUploadFile();
		if (files != null && !files.isEmpty()){
			for (MultipartFile file : files) {
				if (file.isEmpty()) continue;
				
				String originalName = file.getOriginalFilename();
				String uuid = UUID.randomUUID().toString();
				String extension = "";
				
				if (originalName != null && originalName.contains(".")) {
					extension = originalName.substring(originalName.lastIndexOf("."));
				}
				String savedName = uuid + extension;
				
				File saveFile = new File(uploadPath, savedName);
				if(!saveFile.getParentFile().exists()){
					saveFile.getParentFile().mkdirs();
				}
				file.transferTo(saveFile);
				
				BoardFile boardFile = BoardFile.builder()
						.boardId(savedBoard)
						.originalName(originalName)
						.savedName(savedName)
						.build();
				
				boardFileRepository.save(boardFile);
			}
			
		}
		log.info("게시글 작성 완료: {}", savedBoard.getBoardId());
	}
	
	public Page<boardDTO> getBoardList(Integer page, String searchType, String searchWord){
		Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC,"createdDate"));
		Page<board> boardPage = boardRepository.findAllByOrderByCreatedDateDesc(pageable);
		
		if (searchWord == null || searchWord.trim().isEmpty()){
			boardPage = boardRepository.findAllByOrderByCreatedDateDesc(pageable);
		} else {
			switch (searchType){
				case "title":
					boardPage = boardRepository.findByTitleContainingOrderByCreatedDateDesc(searchWord, pageable);
					break;
				case "content":
					boardPage = boardRepository.findByContentContainingOrderByCreatedDateDesc(searchWord, pageable);
					break;
				case "writer":
					boardPage = boardRepository.findByMember_MemberIdContainingOrderByCreatedDateDesc(searchWord, pageable);
					break;
				default:
					boardPage = boardRepository.findAllByOrderByCreatedDateDesc(pageable);
			}
		}
		
		return boardPage.map(board -> boardDTO.builder()
				.boardId(board.getBoardId())
				.title(board.getTitle())
				.writer(board.getMember().getMemberId())
				.createdDate(board.getCreatedDate())
				.viewCount(board.getViewCount())
				.originalFile(board.getOriginalFile())
				.likeCount(recommendRepository.countLike(board.getBoardId()))
				.dislikeCount(recommendRepository.countDislikes(board.getBoardId()))
				.replyCount(replyRepository.countByBoard_BoardId(board.getBoardId()))
				.build());
	}
	public boardDTO getBoard(Integer boardId, boolean isViewed){
		board boardInfo = boardRepository.findById(boardId).orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다"));
		if (boardInfo.getFileList() != null){
			boardInfo.getFileList().size();
		}
		
		if (!isViewed){
			boardInfo.setViewCount(boardInfo.getViewCount() + 1);
		}
		return boardDTO.builder()
				.boardId(boardInfo.getBoardId())
				.writer(boardInfo.getMember().getMemberId())
				.title(boardInfo.getTitle())
				.content(boardInfo.getContent())
				.viewCount(boardInfo.getViewCount())
				.createdDate(boardInfo.getCreatedDate())
				.originalFile(boardInfo.getOriginalFile())
				.savedFile(boardInfo.getSavedFile())
				.likeCount(recommendRepository.countLike(boardInfo.getBoardId()))
				.dislikeCount(recommendRepository.countDislikes(boardInfo.getBoardId()))
				.deckId(boardInfo.getDeckId())
				.fileList(boardInfo.getFileList())
				.build();
	}
	
	public void deleteBoard(Integer boardId, String loginId){
		board boardinfo = boardRepository.findById(boardId).orElseThrow(()->new RuntimeException("글이 존재하지 않습니다"));
		
		if (!boardinfo.getMember().getMemberId().equals(loginId)){
			throw new RuntimeException("삭제 권한이 없습니다");
		}
		List<BoardFile> fileList = boardinfo.getFileList();
		if (fileList != null && !fileList.isEmpty()){
			for (BoardFile file : fileList){
				File diskFile = new File(uploadPath, file.getSavedName());
				
				if (diskFile.exists()){
					diskFile.delete();
				}
			}
		}

		boardRepository.delete(boardinfo);
	}
	public void updateBoard(boardDTO boardDTO, String loginId) throws IOException {
		board boardinfo = boardRepository.findById(boardDTO.getBoardId()).orElseThrow(()->new RuntimeException("글이 존재하지않습니다"));
		
		if (!boardinfo.getMember().getMemberId().equals(loginId)){
			throw new RuntimeException("수정 권한이 없습니다");
		}
		
		boardinfo.setTitle(boardDTO.getTitle());
		boardinfo.setContent(boardDTO.getContent());
		List<Long> deleteIds = boardDTO.getDeleteFileIdList();
		if(deleteIds != null && !deleteIds.isEmpty()) {
			for (Long fileId : deleteIds){
				BoardFile fileEntity = boardFileRepository.findById(fileId).orElse(null);
				
				if (fileEntity != null){
					File diskFile = new File(uploadPath, fileEntity.getSavedName());
					if(diskFile.exists()){
						diskFile.delete();
					}
					boardFileRepository.delete(fileEntity);
				}
			}
		}
		List<MultipartFile> files = boardDTO.getUploadFile();
		if (files != null && !files.isEmpty()){
			for (MultipartFile file :  files){
				if (file.isEmpty()) continue;
				
				String originalName = file.getOriginalFilename();
				String uuid = UUID.randomUUID().toString();
				String extension = "";
				if (originalName != null && originalName.contains(".")) {
					extension = originalName.substring(originalName.lastIndexOf("."));
				}
				String savedName = uuid + extension;
				
				File saveFile = new File(uploadPath, savedName);
				file.transferTo(saveFile);
				
				BoardFile boardFile = BoardFile.builder()
						.boardId(boardinfo)
						.originalName(originalName)
						.savedName(savedName)
						.build();
				
				boardFileRepository.save(boardFile);
			}
		}
		boardRepository.save(boardinfo);
	}
	
}
