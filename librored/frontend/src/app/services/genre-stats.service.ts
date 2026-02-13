import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface GenreStats {
  [genre: string]: number;
}

@Injectable({
  providedIn: 'root'
})
export class GenreStatsService {
  private readonly API_URL = '../api/books/books-per-genre';

  constructor(private http: HttpClient) {}

  getGenreStats(): Observable<GenreStats> {
    return this.http.get<GenreStats>(this.API_URL, {
      withCredentials: true
    });
  }
}