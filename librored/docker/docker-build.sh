#!/bin/bash
# This script builds the Docker image for the LibroRed application

echo "Building LibroRed Docker Image..."

# Clean up any existing containers and images (optional)
echo "Cleaning up existing containers and images..."
docker-compose down --remove-orphans
docker system prune -f

# Build the application image
echo "Building LibroRed application image..."
docker build -t librored-app:latest .

# Verify the build
if [ $? -eq 0 ]; then
    echo "LibroRed Docker image built successfully!"
    echo "Image details:"
    docker images librored-app:latest
else
    echo "Docker build failed!"
    exit 1
fi

echo "To run the application, use: ./docker-run.sh"
echo "Or use docker-compose: docker-compose up -d"