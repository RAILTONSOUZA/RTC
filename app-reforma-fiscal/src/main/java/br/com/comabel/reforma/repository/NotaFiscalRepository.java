package br.com.comabel.reforma.repository;

import br.com.comabel.reforma.model.NotaFiscalResumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotaFiscalRepository extends JpaRepository<NotaFiscalResumo, String> {
    List<NotaFiscalResumo> findByCanceladaFalse();
    List<NotaFiscalResumo> findByCanceladaTrue();
    List<NotaFiscalResumo> findByStatus(String status);
}
