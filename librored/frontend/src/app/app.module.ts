import { BrowserModule } from "@angular/platform-browser";
import { FormsModule } from "@angular/forms";
import { NgModule } from "@angular/core";
import { HttpClientModule, HTTP_INTERCEPTORS } from "@angular/common/http";
import { RouterModule } from "@angular/router";
import { routing } from "./app.routing";
import { NgbModule } from "@ng-bootstrap/ng-bootstrap";

import { AppComponent } from "./app.component";
import { HomeComponent } from "./components/home/home.component";
import { BookDetailComponent } from "./components/books/book-detail.component";
import { BookListComponent } from "./components/books/book-list.component";
import { BookFormComponent } from "./components/books/book-form.component";
import { LoanListComponent } from "./components/loans/loan-list.component";
import { LoanFormComponent } from "./components/loans/loan-form.component";
import { LoginComponent } from "./components/login/login.component";
import { RegisterComponent } from "./components/register/register.component";
import { AdminUsersComponent } from "./components/admin/admin-users.component";
import { AdminDashboardComponent } from "./components/admin/admin-dashboard.component";
import { AdminBooksComponent } from "./components/admin/admin-books/admin-books.component";
import { AdminLoansComponent } from "./components/admin/admin-loans/admin-loans.component";
import { GenreChartComponent } from "./components/genre-chart/genre-chart.component";
import { UserAccountComponent } from "./components/user-account/user-account.component";
import { UserBooksComponent } from "./components/user-books/user-books.component";
import { UserLoansComponent } from "./components/user-loans/user-loans.component";
import { AuthInterceptor } from "./interceptors/auth.interceptor";

@NgModule({
  declarations: [
    AppComponent,
    HomeComponent,
    BookDetailComponent,
    BookListComponent,
    BookFormComponent,
    LoanListComponent,
    LoanFormComponent,
    LoginComponent,
    RegisterComponent,
    AdminUsersComponent,
    AdminDashboardComponent,
    AdminBooksComponent,
    AdminLoansComponent,
    GenreChartComponent,
    UserAccountComponent,
    UserBooksComponent,
    UserLoansComponent,
  ],
  imports: [BrowserModule, FormsModule, HttpClientModule, RouterModule, routing, NgbModule],
  providers: [
    {
      provide: HTTP_INTERCEPTORS,
      useClass: AuthInterceptor,
      multi: true
    }
  ],
  bootstrap: [AppComponent],
})
export class AppModule {}