package net.datasa.Wiki.repository;

import net.datasa.Wiki.domain.entity.EquipEffect;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipEffectRepository extends JpaRepository<EquipEffect, Integer> {
	List<EquipEffect> findBySpellId(Integer spellId);
}
