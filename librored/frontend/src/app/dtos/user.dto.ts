export interface UserDTO {
  id?: number;
  username: string;
  email: string;
  role: string; // Single role: "ROLE_USER" or "ROLE_ADMIN"
}