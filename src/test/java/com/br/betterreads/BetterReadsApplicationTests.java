package com.br.betterreads;

import com.br.betterreads.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class BetterReadsApplicationTests {

    @Test
    void contextLoads() {
    }

}

@ExtendWith(MockitoExtension.class)
class BrukerServiceTest {

    @Mock
    private BrukerRepository UserRepository;

    @InjectMocks
    private BrukerService UserService;

    @Test
    void CreateUser_SaveUserWithHashedPassword() {
        String rawPassord = "passord123";
        User mockUser = new User();
        mockUser.setSalt_Password("randomSalt");
        mockUser.setHashed_Password("hashedValue");

        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User savedUser = UserService.createUser("testuser", "test@example.com", rawPassord);

        assertNotNull(savedUser.getSalt_Password());
        assertNotNull(savedUser.getHashed_Password());
        assertNotEquals(rawPassord, savedUser.getHashed_Password());
        verify(userRepository).save(any(User.class));
    }
}

@ExtendWith(MockitoExtension.class)
class BokServiceTest {

    @Mock
    private BokRepository bokRepository;

    @InjectMocks
    private BokService bokService;

    @Test
    void fetchOrCreateBook {
        String isbn = "97182"
    }
}
