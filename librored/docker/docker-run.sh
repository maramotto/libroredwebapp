#!/bin/bash

# LibroRed Docker Run Script
# This script runs the complete LibroRed application with MySQL database

echo "Starting LibroRed Application with Docker Compose..."

# Check if docker-compose is available
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "Docker Compose is not installed. Please install Docker Compose first."
    exit 1
fi

# Create logs directory if it doesn't exist
mkdir -p logs

# Start the application stack
echo "Starting MySQL database and LibroRed application..."
docker-compose up -d

# Wait for services to be healthy
echo "Waiting for services to start..."
sleep 30

# Check service status
echo "Service Status:"
docker-compose ps

# Show application logs
echo "Recent application logs:"
docker-compose logs --tail=20 librored-app

# Display connection information
echo ""
echo "LibroRed Application Started Successfully!"
echo "Application URL: https://localhost:8443"
echo "API Documentation: https://localhost:8443/swagger-ui.html"
echo "API Docs JSON: https://localhost:8443/v3/api-docs"
echo "MySQL Database: localhost:3306"
echo ""
echo "Useful commands:"
echo "  • View logs: docker-compose logs -f librored-app"
echo "  • Stop services: docker-compose down"
echo "  • Restart services: docker-compose restart"
echo "  • View all services: docker-compose ps"
echo ""
echo "To stop the application: docker-compose down"