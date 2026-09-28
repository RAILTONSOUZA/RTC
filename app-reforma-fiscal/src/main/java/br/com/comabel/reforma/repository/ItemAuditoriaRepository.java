package br.com.comabel.reforma.repository;

import br.com.comabel.reforma.model.ItemAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItemAuditoriaRepository extends JpaRepository<ItemAuditoria, Long> {
    List<ItemAuditoria> findByStatusNot(String status);
    List<ItemAuditoria> findByChNFe(String chNFe);
}
