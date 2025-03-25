package com.br.betterreads;

import com.br.betterreads.config.SecurityConfig;
import com.br.betterreads.model.User;
import com.br.betterreads.repository.UserRepository;
import com.br.betterreads.service.UserService;
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

  /*  @Test
    void createUser_SaveUserWithHashedPassword() {
        String rawPassord = "passord123";
        User mockUser = new User();

        mockUser.setHashed_Password("hashedValue");

        when(userRepository.save(any(User.class))).thenReturn(mockUser);

        assertNotEquals(rawPassord, savedUser.getHashed_Password());
        verify(userRepository).save(any(User.class));
    }*/
}
