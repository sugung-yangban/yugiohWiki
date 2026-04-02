package net.datasa.Wiki.repository;

import net.datasa.Wiki.domain.entity.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, Integer> {
	List<Reply> findByBoard_BoardIdOrderByCreatedDateAsc(Integer boardId);
	List<Reply> findByBoard_BoardIdAndParentReplyIsNullOrderByCreatedDateAsc(Integer boardId);
	int countByBoard_BoardId(Integer boardId);
}
