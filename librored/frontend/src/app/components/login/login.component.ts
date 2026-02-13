import { Component, TemplateRef, ViewChild } from "@angular/core";
import { AuthService } from "../../services/auth.service";
import { NgbModal } from "@ng-bootstrap/ng-bootstrap";
import { Router } from "@angular/router";

@Component({
  selector: "login",
  templateUrl: "./login.component.html",
  styleUrls: ["./login.component.css"]
})
export class LoginComponent {
  @ViewChild("loginErrorModal")
  public loginErrorModal: TemplateRef<void> | undefined;

  constructor(
    public authService: AuthService,
    private modalService: NgbModal,
    private router: Router
  ) {}

  public logIn(user: string, pass: string) {
    this.authService.logIn(user, pass).subscribe({
      next: (response) => {
        console.log("Login successful:", response);
        // Redirect to home page
        this.router.navigate(['/']);
      },
      error: (error) => {
        console.error("Login failed:", error);
        if (this.loginErrorModal) {
          this.modalService.open(this.loginErrorModal, { centered: true });
        }
      }
    });
  }

  public logOut() {
    this.authService.logOut();
  }
}