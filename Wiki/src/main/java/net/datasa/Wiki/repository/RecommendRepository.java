package net.datasa.Wiki.repository;

import net.datasa.Wiki.domain.entity.Recommend;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendRepository extends JpaRepository<Recommend, Integer> {
	Optional<Recommend> findByBoard_BoardIdAndMember_MemberId(Integer boardId, String memberId);
	
	@Query("select count(r) from Recommend r where r.board.boardId = :boardId and r.voteType = true")
	int countLike (@Param("boardId") Integer boardId);
	
	@Query("select count(r) from Recommend r where r.board.boardId = :boardId and r.voteType = false")
	int countDislikes (@Param("boardId")Integer boardId);
}
