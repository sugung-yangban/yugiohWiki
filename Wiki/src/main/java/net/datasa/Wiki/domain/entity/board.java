package net.datasa.Wiki.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "board")
public class board {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "board_id")
	private Integer boardId;
	@Column(name = "title", length = 200)
	private String title;
	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="member_id")
	private wikiMember member;
	@CreatedDate
	@Column(name = "created_date", updatable = false)
	private LocalDateTime createdDate;
	@Column(name="view_count", columnDefinition = "integer default 0")
	private int viewCount;
	@Column(name="original_file")
	private String originalFile;
	@Column(name="saved_file")
	private String savedFile;
	@OneToMany(mappedBy = "boardId", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<BoardFile> fileList = new ArrayList<>();
	@Column(name = "linked_deck_id")
	private Integer deckId;
	@OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Recommend> recommends = new ArrayList<>();
	@OneToMany(mappedBy = "board", cascade = CascadeType.REMOVE, orphanRemoval = true)
	private List<Reply> replies = new ArrayList<>();
}
