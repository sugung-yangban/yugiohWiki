package net.datasa.Wiki.domain.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "recommend")
public class Recommend {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer recommendId;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "board_id")
	private board board;
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private wikiMember member;
	@Column(name="vote_type")
	private Boolean voteType;
}
