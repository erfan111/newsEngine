package ir.open30stem.models;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by Erfan Sharafzadeh on 12/11/16.
 */
public interface SpiderRepository extends MongoRepository<Spider, String> {
}
