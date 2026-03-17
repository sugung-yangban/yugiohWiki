package net.datasa.Wiki.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "wikimember")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class wikiMember {
	@Id
	@Column(name = "member_id", length = 30)
	String memberId;
	@Column(name = "member_password", nullable = false, length = 100)
	String memberPassword;
	@Column(name = "profile_image", length = 100)
	String profileImage;
	@OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
	private List<myDeck> mydecks = new ArrayList<>();
	@OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
	private List<board> boards = new ArrayList<>();
	@OneToMany(mappedBy = "member", cascade = CascadeType.REMOVE)
	private List<Reply> replies = new ArrayList<>();
}
