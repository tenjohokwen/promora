package com.softropic.promora.security.repo;


import com.softropic.promora.common.validation.PhoneNumber;
import com.softropic.promora.common.validation.Provider;
import com.softropic.promora.config.TestConfig;
import com.softropic.promora.security.domain.Address;
import com.softropic.promora.security.domain.User;
import com.softropic.promora.security.repository.UserRepository;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@ActiveProfiles("dev")
@DataJpaTest(properties = {"ledger.database.spy=true", "enable.test.mail=true"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestConfig.class)
@Transactional
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepo;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findOneById() {
        final User user = Instancio.create(User.class);
        user.setId(null);
        user.setPersistentTokens(Collections.emptySet());
        user.setLangKey("en");
        user.setEmail("me@yahoo.com");
        user.setPassword("sixtysixtysixtysixtysixtysixtysixtysixtysixtysixtysixtysixty");
        user.setLogin("me@yahoo.com");
        user.setDateOfBirth(LocalDate.of(1978, 3, 19));
        user.setPhone(new PhoneNumber("01794443151", Provider.MTN, "DE"));
        final Address address = user.getAddresses().stream().findFirst().orElse(null);
        address.setName("abcdAddress");
        user.setAddresses(Set.of(address));
        user.setAuthorities(Set.of());

        final User savedUser = userRepo.save(user);
        final Optional<User> foundUser = userRepo.findOneById(savedUser.getId());

        assertThat(foundUser).isNotEmpty();
    }
}