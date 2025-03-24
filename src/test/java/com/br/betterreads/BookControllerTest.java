package com.br.betterreads;

import com.br.betterreads.controller.BookController;
import com.br.betterreads.model.Book;
import com.br.betterreads.repository.BookRepository;
import com.br.betterreads.service.ApiService;
import com.br.betterreads.service.BookService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.method.annotation.ModelAttributeMethodProcessor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = BookController.class, includeFilters = {
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BookControllerTest.SecurityTestConfig.class)
})
class BookControllerTest {

    @TestConfiguration
    static class TestConfig {
        @Bean
        public BookService bookService() {
            return mock(BookService.class);
        }

        @Bean
        public ApiService apiService() {
            return mock(ApiService.class);
        }

        @Bean
        public BookRepository bookRepository() {
            return mock(BookRepository.class);
        }
    }

    @TestConfiguration
    public static class SecurityTestConfig {
        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .csrf(AbstractHttpConfigurer::disable);
            return http.build();
        }

        @Bean
        public WebMvcConfigurer webMvcConfigurer() {
            return new WebMvcConfigurer() {
                @Override
                public void addInterceptors(InterceptorRegistry registry) {
                    registry.addInterceptor(new HandlerInterceptor() {
                        @Override
                        public void postHandle(HttpServletRequest request, HttpServletResponse response,
                                               Object handler, ModelAndView modelAndView) {
                            if (modelAndView != null) {
                                modelAndView.addObject("username", "testUser");
                            }
                        }
                    });
                }
            };
        }
    }


    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private BookService bookService;

    private Book testBook;
    private List<Book> testBooks;

    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @BeforeEach
    void setUp() {
        testBook = new Book();
        testBook.setApiId(1);
        testBook.setTitle("Test Book");
        testBook.setSubtitle("A Test Subtitle");
        testBook.setAuthor("Test Author");
        testBook.setDescription("This is a test description");
        testBook.setIsbn("1234567890123");
        testBook.setCoverURL("/images/covertemplate.jpg");
        testBook.setLastSync(LocalDateTime.now());
        testBook.setGenre(new String[]{"Fiction", "Test"});
        testBook.setPublicationYear(2023);

        testBooks = new ArrayList<>();
        testBooks.add(testBook);

        Book book2 = new Book();
        book2.setApiId(2);
        book2.setTitle("Another Test Book");
        book2.setSubtitle("Another Test Subtitle");
        book2.setAuthor("Another Author");
        book2.setDescription("Another test description");
        book2.setIsbn("9876543210987");
        book2.setCoverURL("/images/covertemplate.jpg");
        book2.setLastSync(LocalDateTime.now());
        testBooks.add(book2);

        when(bookService.searchBookByIsbn("1234567890123")).thenReturn(testBook);
        when(bookService.searchBookByIsbn("0000000000000")).thenReturn(null);
        when(bookService.searchBooks("Test", "title")).thenReturn(testBooks);
        when(bookService.searchBooks("Unique Title", "title")).thenReturn(List.of(testBook));
        when(bookService.searchBooks("Unknown Title", "title")).thenReturn(new ArrayList<>());
        when(bookService.searchBooks("Author", "author")).thenReturn(testBooks);
        when(bookService.searchBooks("Unknown Author", "author")).thenReturn(new ArrayList<>());
    }

    @Test
    @WithMockUser
    void isbnSearchWithResult() throws Exception {
        mockMvc.perform(get("/search")
                        .param("query", "1234567890123")
                        .param("searchType", "isbn"))
                .andExpect(status().isOk())
                .andExpect(view().name("Bok"))
                .andExpect(model().attributeExists("book"))
                .andExpect(model().attribute("book", testBook))
                .andExpect(model().attributeExists("username"));
    }

    @Test
    @WithMockUser
    void isbnSearchWithNoResult() throws Exception {
        mockMvc.perform(get("/search")
                        .param("query", "0000000000000")
                        .param("searchType", "isbn"))
                .andExpect(status().isOk())
                .andExpect(view().name("searchError"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser
    void titleSearchWithMultipleResults() throws Exception {
        mockMvc.perform(get("/search")
                        .param("query", "Test")
                        .param("searchType", "title"))
                .andExpect(status().isOk())
                .andExpect(view().name("searchResults"))
                .andExpect(model().attributeExists("books"))
                .andExpect(model().attribute("books", testBooks))
                .andExpect(model().attributeExists("query"))
                .andExpect(model().attribute("query", "Test"));
    }

    @Test
    @WithMockUser
    void titleSearchWithSingleResult() throws Exception {
        mockMvc.perform(get("/search")
                        .param("query", "Unique Title")
                        .param("searchType", "title"))
                .andExpect(status().isOk())
                .andExpect(view().name("Bok"))
                .andExpect(model().attributeExists("book"))
                .andExpect(model().attribute("book", testBook));
    }

    @Test
    @WithMockUser
    void titleSearchWithNoResults() throws Exception {
        mockMvc.perform(get("/search")
                        .param("query", "Unknown Title")
                        .param("searchType", "title"))
                .andExpect(status().isOk())
                .andExpect(view().name("searchError"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "No books found for: Unknown Title"));
    }

    @Test
    @WithMockUser
    void authorSearchWithResults() throws Exception {
        mockMvc.perform(get("/search")
                        .param("query", "Author")
                        .param("searchType", "author"))
                .andExpect(status().isOk())
                .andExpect(view().name("searchResults"))
                .andExpect(model().attributeExists("books"))
                .andExpect(model().attribute("books", testBooks));
    }

    @Test
    @WithMockUser
    void authorSearchWithNoResults() throws Exception {
        mockMvc.perform(get("/search")
                        .param("query", "Unknown Author")
                        .param("searchType", "author"))
                .andExpect(status().isOk())
                .andExpect(view().name("searchError"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @WithMockUser
    void viewValidBook() throws Exception {
        mockMvc.perform(get("/book")
                        .param("isbn", "1234567890123"))
                .andExpect(status().isOk())
                .andExpect(view().name("Bok"))
                .andExpect(model().attributeExists("book"))
                .andExpect(model().attribute("book", testBook));
    }

    @Test
    @WithMockUser
    void viewInvalidBook() throws Exception {
        mockMvc.perform(get("/book")
                        .param("isbn", "0000000000000"))
                .andExpect(status().isOk())
                .andExpect(view().name("searchError"))
                .andExpect(model().attributeExists("error"))
                .andExpect(model().attribute("error", "Book not found"));
    }
}