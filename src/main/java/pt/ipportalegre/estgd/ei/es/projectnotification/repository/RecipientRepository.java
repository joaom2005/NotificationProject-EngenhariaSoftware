package pt.ipportalegre.estgd.ei.es.projectnotification.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ipportalegre.estgd.ei.es.projectnotification.entity.Recipient;

@Repository
public interface RecipientRepository
        extends JpaRepository<Recipient, Long> {
}