package net.datasa.Wiki.repository;

import net.datasa.Wiki.domain.entity.FusionRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FusionRecipeRepository extends JpaRepository<FusionRecipe, Integer> {
	List<FusionRecipe> findByFusionMonsterId(Integer fusionMonsterId);
}
