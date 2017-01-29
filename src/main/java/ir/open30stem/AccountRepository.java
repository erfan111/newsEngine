package ir.open30stem;

import ir.open30stem.models.Account;

import java.util.Optional;

/**
 * Created by Erfan Sharafzadeh on 11/17/16.
 */
public interface AccountRepository {
    Optional<Account> findByUsername(String username);
    Account save(Account account);
}
