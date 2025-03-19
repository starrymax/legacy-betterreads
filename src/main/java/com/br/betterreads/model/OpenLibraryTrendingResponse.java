package com.br.betterreads.model;

import java.util.List;

public class OpenLibraryTrendingResponse {


    private List<TrendingBook> works;

    public List<TrendingBook> getWorks() {return works;}
    public void setWorks(List<TrendingBook> works) {this.works = works;}

    public static class TrendingBook {
        private String key;
        private String title;
        private List<String> author_name;
        private Integer first_published_year;
        private Integer cover_i;

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public List<String> getAuthor_name() {
            return author_name;
        }

        public void setAuthor_name(List<String> author_name) {
            this.author_name = author_name;
        }

        public Integer getFirst_published_year() {
            return first_published_year;
        }

        public void setFirst_published_year(Integer first_published_year) {
            this.first_published_year = first_published_year;
        }

        public Integer getCover_i() {
            return cover_i;
        }

        public void setCover_i(Integer cover_i) {
            this.cover_i = cover_i;
        }

        public String getCoverUrl() {
            if(cover_i !=null) {
                return "https://covers.openlibrary.org/b/id/" + cover_i + "-M.jpg";
            }
            return "https://www.nypl.org/scout/_next/image?url=https%3A%2F%2Fdrupal.nypl.org%2Fsites-drupal%2Fdefault%2Ffiles%2Fstyles%2Fmax_width_960%2Fpublic%2Fblogs%2FJ5LVHEL.jpg%3Fitok%3DDkMp1Irh&w=3840&q=90";
        }
    }

}
