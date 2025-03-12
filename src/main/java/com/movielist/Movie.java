package com.movielist;

public class Movie {

    private String name;
    private int date;
    private double score;
    private String posterPath;
    private String director;
    private String releaseDate;
    private String description;

    public Movie(String name, int date, double score) {
        this.name = name;
        this.date = date;
        this.score = score;
    }

    // Getters e Setters
    public String getName() { return name; }
    public int getDate() { return date; }
    public double getScore() { return score; }
    public String getPosterPath() { return posterPath; }
    public String getDirector() { return director; }
    public String getReleaseDate() { return releaseDate; }
    public String getDescription() { return description; }

    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public void setDirector(String director) { this.director = director; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public void setDescription(String description) { this.description = description; }

    @Override
    public String toString() {
        return "Movie{" +
                "name='" + name + '\'' +
                ", date=" + date +
                ", score=" + score +
                ", posterPath='" + posterPath + '\'' +
                ", director='" + director + '\'' +
                ", releaseDate='" + releaseDate + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
