package com.movielist;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.github.cdimascio.dotenv.Dotenv;

public class Main {
    public static void main(String[] args) {

        String filePath = "moviess.xlsx";

        List<Movie> movies = lerExcel(filePath);

        int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);

        for (Movie movie : movies) {
            executor.execute(() -> fetchMovieDetails(movie));
        }

        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Movie m : movies) {
            System.out.println(m);
        }
    }

    public static List<Movie> lerExcel(String fileName){
        List<Movie> list = new ArrayList<>();

        try (InputStream file = Main.class.getClassLoader().getResourceAsStream(fileName);
             Workbook workbook = new XSSFWorkbook(file)) {

            Sheet sheet = workbook.getSheetAt(0);

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                Cell cellName = row.getCell(0);
                Cell cellDate = row.getCell(1);
                Cell cellScore = row.getCell(2);

                if (cellName != null && cellDate != null && cellScore != null) {
                    String name = cellName.getStringCellValue();
                    int date = (int) cellDate.getNumericCellValue();
                    int score = (int) cellScore.getNumericCellValue();

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
        String baseUrl = "https://api.themoviedb.org/3/search/movie?query=";
        String query = movie.getName().replace(" ", "%20");
        String urlString = baseUrl + query + "&api_key=" + apiKey;

        try {
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");

            if (conn.getResponseCode() != 200) {
                System.out.println("Erro na conexão: " + conn.getResponseCode());
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
                movie.setPosterPath("https://image.tmdb.org/t/p/w500" + firstResult.getString("poster_path"));
                movie.setReleaseDate(firstResult.getString("release_date"));
                movie.setDescription(firstResult.getString("overview"));

                fetchDirector(movie, firstResult.getInt("id"), apiKey);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fetchDirector(Movie movie, int movieId, String apiKey) {
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

            for (int i = 0; i < crew.length(); i++) {
                JSONObject person = crew.getJSONObject(i);
                if (person.getString("job").equals("Director")) {
                    movie.setDirector(person.getString("name"));
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class Config {
        private static final Dotenv dotenv = Dotenv.load();

        public static String getApiKey() {
            return dotenv.get("TMDB_API_KEY");
        }
    }
}
