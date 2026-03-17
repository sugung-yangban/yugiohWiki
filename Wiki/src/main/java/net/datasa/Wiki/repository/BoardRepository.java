package net.datasa.Wiki.repository;

import net.datasa.Wiki.domain.entity.board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoardRepository extends JpaRepository<board, Integer> {
	Page<board> findAllByOrderByCreatedDateDesc(Pageable pageable);
}
