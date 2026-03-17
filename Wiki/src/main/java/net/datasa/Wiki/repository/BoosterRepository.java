package net.datasa.Wiki.repository;

import net.datasa.Wiki.domain.entity.boosterpack;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/*boosterpack 테이블을 사용한 쿼리 실행*/
// public interface XxxRepository extends
// JpaRepository <boosterpack(엔티티명), String(타입)>
@Repository
public interface BoosterRepository extends JpaRepository<boosterpack, Integer> {
	@Query("SELECT b FROM boosterpack b WHERE " +
			"(:keyword IS NULL OR b.cardname LIKE %:keyword%) AND " +
			"(:race IS NULL OR :race = '' OR b.race = :race) AND " +
			"(:element IS NULL OR :element = '' OR b.element = :element) AND " +
			"(:level IS NULL OR b.level = :level)" +
			"AND (:cardtype IS NULL OR :cardtype = 'all' OR b.cardtype like %:cardtype%)" +
			"and (:type is null or :type = 'all' or b.type like %:type%)")
	
	Page<boosterpack> findByComplexCondition(
			@Param("keyword") String keyword,
			@Param("race") String race,
			@Param("element") String element,
			@Param("level") Integer level,
			@Param("cardtype") String cardtype,
			@Param("type") String type,
			Pageable pageable
	);
	
	Optional<boosterpack> findByCardname(String cardname);
	Optional<boosterpack> deleteByCardname(String cardname);
	Page<boosterpack> findByCardnameContaining(String keyword, Pageable pageable);
	
	List<boosterpack> findByRarityInOrderByReleasedateDesc(List<String> rarities);
	boosterpack findTop1ByRarityInOrderByReleasedateDesc(List<String> rarities);
	List<boosterpack> findByReleasedateAndRarityIn(LocalDate releaseDate, List<String> rarities);
}