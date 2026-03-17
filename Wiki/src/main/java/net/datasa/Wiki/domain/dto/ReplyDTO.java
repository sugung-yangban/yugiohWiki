package net.datasa.Wiki.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyDTO {
	private Integer replyId;
	private Integer boardId;
	private String memberId;
	private String text;
	private LocalDateTime createdDate;
	private Integer parentId;
	@Builder.Default
	private List<ReplyDTO> childReplies = new ArrayList<>();
}
