package net.datasa.Wiki.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.datasa.Wiki.domain.entity.BoardFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class boardDTO {
	private Integer boardId;
	private String title;
	private String content;
	private String writer;
	private LocalDateTime createdDate;
	private int viewCount;
	private List<MultipartFile> uploadFile;
	private List<BoardFile> fileList;
	private String originalFile;
	private String savedFile;
	private List<Long> deleteFileIdList;
	private Integer likeCount;
	private Integer dislikeCount;
	private Integer deckId;
	private String deckName;
}
