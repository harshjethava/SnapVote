package net.SnapVote.snapVote.config;

import com.mongodb.client.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

@Component
public class DynamicMongoService {

    @Autowired
    private MongoClient mongoClient;

    // Returns MongoTemplate for selected election area
    public MongoTemplate getMongoTemplate(String area) {
        String dbName = "election_" + area.toLowerCase();
        return new MongoTemplate(mongoClient, dbName);
    }
}
