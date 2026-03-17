package net.datasa.Wiki.repository;

import net.datasa.Wiki.domain.entity.wikiMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface wikiMemberRepository extends JpaRepository<wikiMember, String> {
}
