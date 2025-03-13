package com.movielist;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.*;

import org.json.JSONArray;
import org.json.JSONObject;
import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
public class Main {
    private static final List<Movie> movies = new CopyOnWriteArrayList<>(); // Lista global de filmes

    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
        carregarFilmes();
    }

    public static void carregarFilmes() {
        String filePath = "movies.xlsx";
        List<Movie> loadedMovies = lerExcel(filePath);
        movies.clear();

        Collections.shuffle(loadedMovies); // Embaralha os filmes ao iniciar
        movies.addAll(loadedMovies);

        int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (Movie movie : movies) {
            executor.execute(() -> {
                if (movie.getPosterPath() == null || movie.getPosterPath().isEmpty()) {
                    fetchMovieDetails(movie);
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static List<Movie> getMovies() {
        return movies;
    }

    public static List<Movie> getPaginatedMovies(int page, int size) {
        int startIndex = (page - 1) * size;
        int endIndex = Math.min(startIndex + size, movies.size());

        if (startIndex >= movies.size()) {
            return Collections.emptyList();
        }

        return movies.subList(startIndex, endIndex);
    }

    public static List<Movie> lerExcel(String fileName) {
        List<Movie> list = new ArrayList<>();

        try (InputStream file = Main.class.getClassLoader().getResourceAsStream(fileName);
             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Ignorar cabeçalho

                Cell cellName = row.getCell(0);
                Cell cellDate = row.getCell(1);
                Cell cellScore = row.getCell(2);

                if (cellName != null && cellDate != null && cellScore != null) {
                    String name = cellName.getCellType() == CellType.STRING ?
                            cellName.getStringCellValue().trim() :
                            String.valueOf((int) cellName.getNumericCellValue());

                    if (name.isEmpty()) continue;

                    int date = 0;
                    if (cellDate.getCellType() == CellType.NUMERIC) {
                        date = (int) cellDate.getNumericCellValue();
                    } else if (cellDate.getCellType() == CellType.STRING && !cellDate.getStringCellValue().trim().isEmpty()) {
                        try {
                            date = Integer.parseInt(cellDate.getStringCellValue().trim());
                        } catch (NumberFormatException e) {
                            System.err.println("Erro ao converter data: " + cellDate.getStringCellValue());
                        }
                    }

                    double score = 0.0;
                    if (cellScore.getCellType() == CellType.NUMERIC) {
                        score = cellScore.getNumericCellValue();
                    } else if (cellScore.getCellType() == CellType.STRING && !cellScore.getStringCellValue().trim().isEmpty()) {
                        String scoreStr = cellScore.getStringCellValue().trim().replace(",", ".");
                        try {
                            score = Double.parseDouble(scoreStr);
                        } catch (NumberFormatException e) {
                            System.err.println("Erro ao converter nota: " + scoreStr);
                        }
                    }

                    Movie movie = new Movie(name, date, score);
                    list.add(movie);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }

    public static void fetchMovieDetails(Movie movie) {
        String apiKey = Config.getApiKey();
        String query = movie.getName().replace(" ", "%20");
        String searchUrl = "https://api.themoviedb.org/3/search/movie?query=" + query + "&api_key=" + apiKey + "&language=pt-BR";

        try {
            URL url = new URL(searchUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                return;
            }

            Scanner scanner = new Scanner(url.openStream());
            StringBuilder jsonString = new StringBuilder();
            while (scanner.hasNext()) {
                jsonString.append(scanner.nextLine());
            }
            scanner.close();

            JSONObject json = new JSONObject(jsonString.toString());
            JSONArray results = json.getJSONArray("results");

            if (results.length() > 0) {
                JSONObject firstResult = results.getJSONObject(0);
                int movieId = firstResult.getInt("id");

                String detailsUrl = "https://api.themoviedb.org/3/movie/" + movieId + "?api_key=" + apiKey + "&language=pt-BR";
                URL detailsEndpoint = new URL(detailsUrl);
                HttpURLConnection detailsConn = (HttpURLConnection) detailsEndpoint.openConnection();
                detailsConn.setRequestMethod("GET");
                detailsConn.setRequestProperty("Accept", "application/json");

                Scanner detailsScanner = new Scanner(detailsEndpoint.openStream());
                StringBuilder detailsJsonString = new StringBuilder();
                while (detailsScanner.hasNext()) {
                    detailsJsonString.append(detailsScanner.nextLine());
                }
                detailsScanner.close();

                JSONObject movieDetails = new JSONObject(detailsJsonString.toString());

                movie.setPosterPath("https://image.tmdb.org/t/p/w500" + firstResult.optString("poster_path", ""));
                movie.setReleaseDate(movieDetails.optString("release_date", "Desconhecido"));
                movie.setDescription(movieDetails.optString("overview", ""));
                movie.setDuration(movieDetails.optInt("runtime", 0)); // Duração do filme
                movie.setImdbRating(movieDetails.optDouble("vote_average", 0.0)); // Nota IMDb

                fetchDirectorAndActors(movie, movieId, apiKey);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fetchDirectorAndActors(Movie movie, int movieId, String apiKey) {
        String urlString = "https://api.themoviedb.org/3/movie/" + movieId + "/credits?api_key=" + apiKey;

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            Scanner scanner = new Scanner(url.openStream());
            StringBuilder jsonString = new StringBuilder();
            while (scanner.hasNext()) {
                jsonString.append(scanner.nextLine());
            }
            scanner.close();

            JSONObject json = new JSONObject(jsonString.toString());
            JSONArray crew = json.getJSONArray("crew");
            JSONArray cast = json.getJSONArray("cast");

            for (int i = 0; i < crew.length(); i++) {
                JSONObject person = crew.getJSONObject(i);
                if (person.getString("job").equals("Director")) {
                    movie.setDirector(person.optString("name", "Desconhecido"));
                    break;
                }
            }

            List<String> actors = new ArrayList<>();
            for (int i = 0; i < Math.min(5, cast.length()); i++) { // Pega os 5 primeiros atores
                JSONObject actor = cast.getJSONObject(i);
                actors.add(actor.optString("name", "Desconhecido"));
            }
            movie.setActors(actors);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String formatYear(String date) {
        if (date == null || date.isEmpty()) return "Desconhecido";
        String[] parts = date.split("-");
        return (parts.length >= 1) ? parts[0] : date;
    }

    public static class Config {
        private static final Dotenv dotenv = Dotenv.load();

        public static String getApiKey() {
            return dotenv.get("TMDB_API_KEY");
        }
    }
}
