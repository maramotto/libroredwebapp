export interface BookDTO {
  id?: number;
  title: string;
  description: string;
  author?: string;
  genre?: string;
  hasCoverImage: boolean;
  owner?: {
    id: number;
    username: string;
  };
  shops?: ShopBasicDTO[];
}

export interface ShopBasicDTO {
  id: number;
  name: string;
  address: string;
}