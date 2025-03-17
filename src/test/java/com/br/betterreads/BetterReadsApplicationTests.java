package com.br.betterreads;

import com.br.betterreads.model.User;
import com.br.betterreads.repository.BokRepository;
import com.br.betterreads.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class BetterReadsApplicationTests {

    @Test
    void contextLoads() {
    }
}

@ExtendWith(MockitoExtension.class)
class BrukerServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserRepository userService;

    @Test
    void createUser_SaveUserWithHashedPassword() {
        String rawPassord = "passord123";
        User mockUser = new User();
        mockUser.setSalt_Password("randomSalt");;
        mockUser.setHashed_Password("hashedValue");

        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        User savedUser = UserService.createUser("testuser", "test@example.com", rawPassord);

        assertNotNull(savedUser.getSalt_Password());
        assertNotNull(savedUser.getSalt_Password());
        assertNotEquals(rawPassord, savedUser.getHashed_Password());
        verify(userRepository).save(any(User.class));
    }
}