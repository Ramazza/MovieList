package com.movielist;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/movies")
@CrossOrigin(origins = "*")
public class MovieController {

    @GetMapping
    public List<Movie> getMovies(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        List<Movie> movies = Main.getMovies();

          int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, movies.size());

        if (startIndex >= movies.size()) {
            System.out.println("Nenhum filme encontrado para essa página.");
            return List.of();
        }

        List<Movie> paginatedMovies = movies.subList(startIndex, endIndex);

        return paginatedMovies;
    }

    @GetMapping("/totalPages")
    public int getTotalPages(@RequestParam(defaultValue = "10") int size) {
        int totalMovies = Main.getMovies().size();
        int totalPages = (int) Math.ceil((double) totalMovies / size);

        return totalPages;
    }
}
