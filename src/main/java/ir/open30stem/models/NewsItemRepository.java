package ir.open30stem.models;

import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;


/**
 * Created by Erfan Sharafzadeh on 11/17/16.
 */
public interface NewsItemRepository extends MongoRepository<newsItem, String> {

    List<newsItem> findByFinishedOrIsModified(boolean finished, boolean isModified);

    newsItem findById(String id);

    List<newsItem> findByPressAndFinishedOrderByNewsIdDesc(String p, Boolean f, Pageable pageable);

    List<newsItem> findByCategoryAndFinishedOrderByNewsIdDesc(String p, Boolean f, Pageable pageable);

    @Query(value = "{ 'finished' : true }")
    List<newsItem> findAllLimit(Pageable pageable);


}
