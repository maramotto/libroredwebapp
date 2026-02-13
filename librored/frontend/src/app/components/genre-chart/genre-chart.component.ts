import { Component, OnInit, OnDestroy, AfterViewInit, ViewChild, ElementRef } from '@angular/core';
import { GenreStatsService, GenreStats } from '../../services/genre-stats.service';
import { Chart, ChartConfiguration, registerables } from 'chart.js';

@Component({
  selector: 'app-genre-chart',
  templateUrl: './genre-chart.component.html',
  styleUrl: './genre-chart.component.css'
})
export class GenreChartComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('chartCanvas', { static: false }) chartCanvas!: ElementRef<HTMLCanvasElement>;

  chart: Chart | null = null;
  loading = true;
  errorMessage = '';

  constructor(private genreStatsService: GenreStatsService) {
    Chart.register(...registerables);
  }

  ngOnInit(): void {
    // Component initialization
  }

  ngAfterViewInit(): void {
    this.loadChartData();
  }

  ngOnDestroy(): void {
    if (this.chart) {
      this.chart.destroy();
    }
  }

  private loadChartData(): void {
    this.loading = true;
    this.errorMessage = '';

    this.genreStatsService.getGenreStats().subscribe({
      next: (data: GenreStats) => {
        this.createChart(data);
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading chart data:', error);
        this.errorMessage = 'Failed to load genre statistics';
        this.loading = false;
      }
    });
  }

  private createChart(data: GenreStats): void {
    if (!this.chartCanvas || !this.chartCanvas.nativeElement) {
      console.error('Chart canvas is not available');
      return;
    }

    const genres = Object.keys(data);
    const counts = Object.values(data);

    const ctx = this.chartCanvas.nativeElement;

    if (this.chart) {
      this.chart.destroy();
    }

    const config: ChartConfiguration = {
      type: 'bar',
      data: {
        labels: genres,
        datasets: [{
          label: 'Books per Genre',
          data: counts,
          backgroundColor: 'rgba(54, 162, 235, 0.6)',
          borderColor: 'rgba(54, 162, 235, 1)',
          borderWidth: 1,
          barPercentage: 0.7
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
          x: {
            ticks: {
              maxRotation: 45,
              minRotation: 30
            }
          },
          y: {
            beginAtZero: true
          }
        },
        plugins: {
          legend: {
            display: true
          }
        }
      }
    };

    this.chart = new Chart(ctx, config);
  }

  retryLoad(): void {
    this.loadChartData();
  }
}