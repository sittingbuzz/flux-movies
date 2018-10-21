package io.sittingbuzz.fluxmovies;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.*;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@SpringBootApplication
public class FluxMoviesApplication {

    public static void main(String[] args) {
		SpringApplication.run(FluxMoviesApplication.class, args);
	}

	@Bean
	CommandLineRunner init(MovieRepoitory movieRepoitory) {
	    return args -> {
	        movieRepoitory.deleteAll()
                    .subscribe(null, null, () ->
                            Stream.of("Braveheart,Cosy Dens,Downfall,Gone girl,Little Big Man,Misery,Prisoners,The Endless".split(","))
                                .map(title -> new Movie(UUID.randomUUID().toString(), title, getRandomGenre()))
                                .forEach(movie -> movieRepoitory.save(movie).subscribe(System.out::println)));
        };
    }

    @Bean
    RouterFunction<ServerResponse> routes(MovieService movieService) {
        return route(GET("/movies"),
                        req -> ok().body(movieService.findAll(), Movie.class))
                .and(route(GET("/movies/{id}"),
                        req -> ok().body(movieService.findById(req.pathVariable("id")), Movie.class)))
                .and(route(GET("/movies/{id}/events"),
                        req -> ok().contentType(MediaType.TEXT_EVENT_STREAM).body(
                                movieService.findById(req.pathVariable("id"))
                                            .flatMapMany(movieService::streamStreams), MovieEvent.class)));
    }

    private String getRandomGenre() {
        String[] genre = "Horror,Thriller,Adventure,Western,Romance,Action,Science Fiction".split(",");
        return genre[new Random().nextInt(genre.length)];
    }

}

/*
@RestController
class MovieRestController {

    private MovieService movieService;

    public MovieRestController(MovieService movieService) {
        this.movieService = movieService;
    }

    @GetMapping("/movies")
    public Flux<Movie> getMovies() {
        return movieService.findAll();
    }

    @GetMapping("/movies/{id}")
    public Mono<Movie> getMovie(@PathVariable String id) {
        return movieService.findById(id);
    }

    @GetMapping(value = "/movies/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<MovieEvent> getMovieEvent(@PathVariable String id) {
        return movieService.findById(id)
                .flatMapMany(movieService::streamStreams);
    }

}
*/

@Service
class MovieService {

    private MovieRepoitory movieRepoitory;

    public MovieService(MovieRepoitory movieRepoitory) {
        this.movieRepoitory = movieRepoitory;
    }

    Flux<Movie> findAll() {
        return movieRepoitory.findAll();
    }

    Mono<Movie> findById(String id) {
        return movieRepoitory.findById(id);
    }

    Flux<MovieEvent> streamStreams(Movie movie) {
        Flux<Long> interval = Flux.interval(Duration.ofSeconds(1));
        Flux<MovieEvent> events = Flux.fromStream(Stream.generate(() -> new MovieEvent(new Date(), getRandomName(), movie)));
        return interval.zipWith(events)
                .map(Tuple2::getT2);
    }

    private String getRandomName() {
        String[] names = "Boris,Brice,London,Christel,Chanel,Kimberley,Donovan".split(",");
        return names[new Random().nextInt(names.length)];
    }
}


@Repository
interface MovieRepoitory extends ReactiveMongoRepository<Movie, String> {

}

@Data@AllArgsConstructor@NoArgsConstructor
class MovieEvent {
    private Date date;
    private String name;
    private Movie movie;
}

@Document
@Data@AllArgsConstructor@NoArgsConstructor
class Movie {
    @Id
    private String id;
    private String title;
    private String genre;
}