package com.movielist;

import java.util.List;

public class Movie {
    private String name;
    private int date;
    private double score;
    private String posterPath;
    private String director;
    private List<String> actors;  // Lista de atores principais
    private int duration;  // Duração do filme em minutos
    private double imdbRating;  // Nota IMDb
    private String description;
    private String opinion;  // Opinião do usuário
    private String releaseDate;


    public Movie(String name, int date, double score) {
        this.name = name;
        this.date = date;
        this.score = score;
        this.opinion = ""; // Inicializa a opinião como vazia
    }

    // Getters e Setters
    public String getName() { return name; }
    public int getDate() { return date; }
    public double getScore() { return score; }
    public String getPosterPath() { return posterPath; }
    public String getDirector() { return director; }
    public List<String> getActors() { return actors; }
    public int getDuration() { return duration; }
    public double getImdbRating() { return imdbRating; }
    public String getDescription() { return description; }
    public String getOpinion() { return opinion; }
    public String getReleaseDate() { return releaseDate; };

    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public void setDirector(String director) { this.director = director; }
    public void setActors(List<String> actors) { this.actors = actors; }
    public void setDuration(int duration) { this.duration = duration; }
    public void setImdbRating(double imdbRating) { this.imdbRating = imdbRating; }
    public void setDescription(String description) { this.description = description; }
    public void setOpinion(String opinion) { this.opinion = opinion; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }


    @Override
    public String toString() {
        return "Movie{" +
                "name='" + name + '\'' +
                ", date=" + date +
                ", score=" + score +
                ", posterPath='" + posterPath + '\'' +
                ", director='" + director + '\'' +
                ", actors=" + actors +
                ", duration=" + duration +
                ", imdbRating=" + imdbRating +
                ", description='" + description + '\'' +
                ", opinion='" + opinion + '\'' +
                '}';
    }
}
