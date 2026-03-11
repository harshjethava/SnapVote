package net.SnapVote.snapVote.service;

import net.SnapVote.snapVote.config.DynamicMongoService;
import net.SnapVote.snapVote.model.ContactUs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

@Service
public class ContactUsDataService {

    @Autowired
    private DynamicMongoService mongoService;

    public void createContactUs(ContactUs contactUs, String area)
    {
        MongoTemplate mongoTemplate = mongoService.getMongoTemplate(area);
        mongoTemplate.save(contactUs, "contactUs");
    }

}
