package net.SnapVote.snapVote.repository;

import net.SnapVote.snapVote.model.ContactUs;
import net.SnapVote.snapVote.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ContactUsRepository extends MongoRepository<ContactUs,String> {

    Optional<ContactUs> findByEmail(String email);
}
