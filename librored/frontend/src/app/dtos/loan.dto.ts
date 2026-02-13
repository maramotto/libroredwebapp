export interface BookBasicDTO {
  id: number;
  title: string;
  author: string;
}

export interface UserBasicDTO {
  id: number;
  username: string;
}

export interface LoanDTO {
  id?: number;
  book: BookBasicDTO;
  lender: UserBasicDTO;
  borrower: UserBasicDTO;
  startDate: string; // Format: YYYY-MM-DD
  endDate?: string; // Format: YYYY-MM-DD, optional
  status: LoanStatus;
}

export enum LoanStatus {
  Active = 'Active',
  Completed = 'Completed'
}

export interface LoanRequest {
  book: BookBasicDTO;
  lender: UserBasicDTO;
  borrower: UserBasicDTO;
  startDate: string;
  endDate?: string;
  status: LoanStatus;
}