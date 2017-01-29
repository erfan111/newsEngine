package ir.open30stem;

import ir.open30stem.models.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.Optional;

/**
 * Created by Erfan Sharafzadeh on 11/17/16.
 */
@Repository
public class AccountRepositoryImpl implements AccountRepository {
    private static final String KEY = "Account";

    private RedisTemplate<String, Account> redisTemplate;
    private HashOperations hashOps;

    @Autowired
    public AccountRepositoryImpl(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    private void init() {
        hashOps = redisTemplate.opsForHash();
    }

    @Override
    public Optional<Account> findByUsername(String username) {
        Account account = new Account(username, (String) hashOps.get(KEY, username));
        return Optional.of(account);

    }

    @Override
    public Account save(Account account) {
        hashOps.put(KEY, account.username, account.token);
        return account;
    }
}
