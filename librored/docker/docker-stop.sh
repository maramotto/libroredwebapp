#!/bin/bash

# LibroRed Docker Stop Script
# This script stops the LibroRed application and cleans up Docker resources

echo "Stopping LibroRed Application..."

# Stop and remove containers
echo "Stopping Docker Compose services..."
docker-compose down

# Optional: Remove volumes (uncomment to delete database data)
# echo "Removing volumes..."
# docker-compose down -v

# Optional: Clean up Docker system (uncomment for complete cleanup)
# echo "Cleaning up Docker system..."
# docker system prune -f

echo "LibroRed application stopped successfully!"
echo "Database data is preserved in Docker volumes."
echo ""
echo "To restart: ./docker-run.sh"
echo "To remove all data: docker-compose down -v"